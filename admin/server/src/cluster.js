const { getPool } = require('./db');
const { haversineMeters } = require('./geo');
const { config } = require('./config');

function nearestCluster(p, clusters, radiusM) {
  let best = -1;
  let bestD = Infinity;
  for (let i = 0; i < clusters.length; i++) {
    const d = haversineMeters({ lat: clusters[i].center_lat, lng: clusters[i].center_lng }, p);
    if (d <= radiusM && d < bestD) { bestD = d; best = i; }
  }
  return best;
}

async function loadClusters(p, deviceId) {
  const [rows] = await p.query(
    'SELECT id, center_lat, center_lng, point_count, first_seen, last_seen FROM device_clusters WHERE device_id = ? ORDER BY first_seen ASC',
    [deviceId]
  );
  return rows;
}

async function assignToClusters(deviceId, points, radiusM) {
  const p = getPool();
  let clusters = await loadClusters(p, deviceId);
  for (const pt of points) {
    const i = nearestCluster(pt, clusters, radiusM);
    if (i >= 0) {
      const c = clusters[i];
      const n = c.point_count + 1;
      const newLat = (c.center_lat * c.point_count + pt.lat) / n;
      const newLng = (c.center_lng * c.point_count + pt.lng) / n;
      await p.query(
        'UPDATE device_clusters SET center_lat=?, center_lng=?, point_count=?, last_seen=? WHERE id=?',
        [newLat, newLng, n, pt.seenAt, c.id]
      );
      c.center_lat = newLat; c.center_lng = newLng; c.point_count = n; c.last_seen = pt.seenAt;
    } else {
      const [r] = await p.query(
        'INSERT INTO device_clusters (device_id, center_lat, center_lng, point_count, first_seen, last_seen) VALUES (?,?,?,?,?,?)',
        [deviceId, pt.lat, pt.lng, 1, pt.seenAt, pt.seenAt]
      );
      clusters.push({ id: r.insertId, center_lat: pt.lat, center_lng: pt.lng, point_count: 1, first_seen: pt.seenAt, last_seen: pt.seenAt });
    }
  }
  await updateDeviceFlags(p, deviceId);
  return clusters.length;
}

async function updateDeviceFlags(p, deviceId) {
  const [rows] = await p.query('SELECT COUNT(*) AS c FROM device_clusters WHERE device_id = ?', [deviceId]);
  const count = rows[0].c;
  await p.query('UPDATE devices SET cluster_count = ?, is_key = ? WHERE id = ?', [count, count >= 2 ? 1 : 0, deviceId]);
}

async function recomputeAll(radiusM) {
  const p = getPool();
  await p.query('DELETE FROM device_clusters');
  const [devices] = await p.query('SELECT id FROM devices');
  for (const d of devices) {
    const [sights] = await p.query(
      'SELECT lat, lng, seen_at FROM sightings WHERE device_id = ? ORDER BY seen_at ASC',
      [d.id]
    );
    const points = sights.map((s) => ({ lat: s.lat, lng: s.lng, seenAt: s.seen_at }));
    await assignToClusters(d.id, points, radiusM);
  }
}

module.exports = { assignToClusters, recomputeAll, updateDeviceFlags };
