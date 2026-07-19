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
  const [existing] = await p.query('SELECT id FROM devices WHERE device_type = ? AND device_key = ?', [type, key]);
  if (existing.length > 0) {
    await p.query(
      'UPDATE devices SET name=?, extra=?, last_seen=?, last_lat=?, last_lng=? WHERE id=?',
      [name, extra, seenAt, lat, lng, existing[0].id]
    );
    return existing[0].id;
  }
  const [r] = await p.query(
    'INSERT INTO devices (device_type, device_key, name, extra, first_seen, last_seen, total_count, cluster_count, last_lat, last_lng) VALUES (?,?,?,?,?,?,0,0,?,?)',
    [type, key, name, extra, seenAt, seenAt, lat, lng]
  );
  return r.insertId;
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

router.post('/upload', async (req, res) => {
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
