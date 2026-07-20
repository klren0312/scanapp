const test = require('node:test');
const assert = require('node:assert');
const { assignToClusters, recomputeAll } = require('../src/cluster');
const { getPool, initSchema } = require('../src/db');

test.before(async () => { await initSchema(); });

test('assignToClusters: two close points => 1 cluster', async () => {
  const p = getPool();
  await p.query('DELETE FROM device_clusters');
  await p.query('DELETE FROM devices');
  const [r] = await p.query(
    'INSERT INTO devices (device_type, device_key, name, first_seen, last_seen, last_lat, last_lng) VALUES (?,?,?,NOW(),NOW(),?,?)',
    ['wifi', 'AA:AA', 'x', 39.9, 116.4]
  );
  const id = r.insertId;
  const n = await assignToClusters(id, [
    { lat: 39.900, lng: 116.400, seenAt: new Date('2026-01-01T00:00:00Z') },
    { lat: 39.9001, lng: 116.4001, seenAt: new Date('2026-01-01T00:01:00Z') },
  ], 500);
  assert.equal(n, 1);
});

test('assignToClusters: far apart points => 2 clusters', async () => {
  const p = getPool();
  await p.query('DELETE FROM device_clusters');
  await p.query('DELETE FROM devices');
  const [r] = await p.query(
    'INSERT INTO devices (device_type, device_key, name, first_seen, last_seen, last_lat, last_lng) VALUES (?,?,?,NOW(),NOW(),?,?)',
    ['wifi', 'BB:BB', 'y', 39.9, 116.4]
  );
  const id = r.insertId;
  const n = await assignToClusters(id, [
    { lat: 39.900, lng: 116.400, seenAt: new Date('2026-01-01T00:00:00Z') },
    { lat: 40.900, lng: 117.400, seenAt: new Date('2026-01-01T01:00:00Z') },
  ], 500);
  assert.equal(n, 2);
});

test('recomputeAll: rebuilds clusters and sets is_key', async () => {
  const p = getPool();
  await p.query('DELETE FROM device_clusters');
  await p.query('DELETE FROM sightings');
  await p.query('DELETE FROM devices');
  const [r] = await p.query(
    'INSERT INTO devices (device_type, device_key, name, first_seen, last_seen, total_count, last_lat, last_lng) VALUES (?,?,?,NOW(),NOW(),2,?,?)',
    ['wifi', 'CC:CC', 'z', 39.9, 116.4]
  );
  const id = r.insertId;
  await p.query(
    'INSERT INTO sightings (device_id, uploader_id, lat, lng, `signal`, seen_at) VALUES (?,?,?,?,?,?)',
    [id, 'u1', 39.900, 116.400, -50, new Date('2026-01-01T00:00:00Z')]
  );
  await p.query(
    'INSERT INTO sightings (device_id, uploader_id, lat, lng, `signal`, seen_at) VALUES (?,?,?,?,?,?)',
    [id, 'u1', 40.900, 117.400, -50, new Date('2026-01-01T01:00:00Z')]
  );
  await recomputeAll(500);
  const [rows] = await p.query('SELECT cluster_count, is_key FROM devices WHERE id = ?', [id]);
  assert.equal(rows[0].cluster_count, 2);
  assert.equal(rows[0].is_key, 1);
});
