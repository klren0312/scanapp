const express = require('express');
const { getPool } = require('../db');
const { config } = require('../config');
const { assignToClusters } = require('../cluster');

const router = express.Router();

function normKey(k) { return (k || '').trim().toUpperCase(); }
function isInvalidCoord(lat, lng) {
  return lat == null || lng == null || isNaN(lat) || isNaN(lng) || (lat === 0 && lng === 0);
}

async function getOrCreateDevice(p, type, key, name, extra, seenAt, lat, lng) {
  // INSERT ... ON DUPLICATE KEY UPDATE keeps the lookup-or-create atomic so concurrent uploads of
  // the same new device can't both SELECT-miss and then one 500 on ER_DUP_ENTRY. first_seen only
  // set on insert; last_seen/last_lat/last_lng advance only when this sighting is newer.
  const [r] = await p.query(
    `INSERT INTO devices (device_type, device_key, name, extra, first_seen, last_seen, total_count, cluster_count, is_key, last_lat, last_lng)
     VALUES (?,?,?,?,?,?,0,0,0,?,?)
     ON DUPLICATE KEY UPDATE
       name = VALUES(name),
       extra = VALUES(extra),
       last_seen = GREATEST(last_seen, VALUES(last_seen)),
       last_lat = IF(VALUES(last_seen) >= last_seen, VALUES(last_lat), last_lat),
       last_lng = IF(VALUES(last_seen) >= last_seen, VALUES(last_lng), last_lng)`,
    [type, key, name, extra, seenAt, seenAt, lat, lng]
  );
  // insertId is the existing row id for ON DUPLICATE KEY UPDATE on mysql2 when using
  // `affectedRows`; fetch the id explicitly to be safe across versions.
  if (r && r.insertId && r.affectedRows === 1) return r.insertId;
  const [rows] = await p.query('SELECT id FROM devices WHERE device_type = ? AND device_key = ?', [type, key]);
  return rows[0].id;
}

async function ingestBatch(p, type, items, uploaderId, radiusM) {
  let inserted = 0, duplicates = 0;
  const toCluster = {};
  for (const it of items) {
    const key = normKey(it.bssid || it.address);
    if (!key) continue;
    if (isInvalidCoord(it.lat, it.lng)) continue;
    const name = it.ssid || it.name || '';
    const extra = type === 'wifi' ? JSON.stringify({ frequency: it.frequency || 0 }) : JSON.stringify({ deviceType: it.deviceType || '' });
    const seenAt = new Date(it.timestamp);
    const deviceId = await getOrCreateDevice(p, type, key, name, extra, seenAt, it.lat, it.lng);
    try {
      await p.query(
        'INSERT INTO sightings (device_id, uploader_id, lat, lng, `signal`, seen_at) VALUES (?,?,?,?,?,?)',
        [deviceId, uploaderId, it.lat, it.lng, it.signal ?? 0, seenAt]
      );
      inserted++;
      await p.query('UPDATE devices SET total_count = total_count + 1 WHERE id = ?', [deviceId]);
      (toCluster[deviceId] = toCluster[deviceId] || []).push({ lat: it.lat, lng: it.lng, seenAt });
    } catch (e) {
      if (e.code === 'ER_DUP_ENTRY') duplicates++;
      else throw e;
    }
  }
  for (const deviceId of Object.keys(toCluster)) {
    await assignToClusters(Number(deviceId), toCluster[deviceId], radiusM);
  }
  return { inserted, duplicates };
}

router.post('/', async (req, res) => {
  try {
    const token = req.headers['x-upload-token'];
    if (token !== config.uploadToken) return res.status(401).json({ error: 'invalid upload token' });
    const body = req.body || {};
    const uploaderId = (body.uploaderId || 'unknown').toString().slice(0, 64);
    const radiusM = config.clusterRadiusM;
    const p = getPool();
    let inserted = 0, duplicates = 0;
    if (Array.isArray(body.wifi)) {
      const r = await ingestBatch(p, 'wifi', body.wifi, uploaderId, radiusM);
      inserted += r.inserted; duplicates += r.duplicates;
    }
    if (Array.isArray(body.bluetooth)) {
      const r = await ingestBatch(p, 'bluetooth', body.bluetooth, uploaderId, radiusM);
      inserted += r.inserted; duplicates += r.duplicates;
    }
    res.json({ inserted, duplicates });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: 'internal error' });
  }
});

module.exports = { router, ingestBatch };
