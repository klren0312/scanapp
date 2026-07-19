const express = require('express');
const { getPool } = require('../db');
const { authMiddleware } = require('../auth');
const { toGcj02IfChina } = require('../geo');

const router = express.Router();
router.use(authMiddleware);

router.get('/points', async (req, res) => {
  const p = getPool();
  const { type, keyOnly } = req.query;
  const conds = []; const params = [];
  if (type) { conds.push('device_type = ?'); params.push(type); }
  if (keyOnly === '1') { conds.push('is_key = 1'); }
  const where = conds.length ? `WHERE ${conds.join(' AND ')}` : '';
  const [rows] = await p.query(`SELECT id, device_type, device_key, name, cluster_count, is_key, last_lat, last_lng, last_seen FROM devices ${where}`, params);
  const points = rows.map((r) => { const g = toGcj02IfChina(r.last_lat, r.last_lng); return { id: r.id, type: r.device_type, key: r.device_key, name: r.name, clusterCount: r.cluster_count, isKey: r.is_key === 1, lat: g.lat, lng: g.lng, lastSeen: r.last_seen }; });
  res.json({ points });
});

module.exports = { router };
