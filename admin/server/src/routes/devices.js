const express = require('express');
const { getPool } = require('../db');
const { authMiddleware } = require('../auth');
const { toGcj02IfChina } = require('../geo');

const router = express.Router();
router.use(authMiddleware);

router.get('/', async (req, res) => {
  const p = getPool();
  const { type, keyword, keyOnly, page = '1', pageSize = '20' } = req.query;
  const conds = []; const params = [];
  if (type) { conds.push('device_type = ?'); params.push(type); }
  if (keyword) { conds.push('(device_key LIKE ? OR name LIKE ?)'); params.push(`%${keyword}%`, `%${keyword}%`); }
  if (keyOnly === '1') { conds.push('is_key = 1'); }
  const where = conds.length ? `WHERE ${conds.join(' AND ')}` : '';
  const pg = Math.max(1, parseInt(page, 10) || 1);
  const sz = Math.min(200, Math.max(1, parseInt(pageSize, 10) || 20));
  const [rows] = await p.query(`SELECT id, device_type, device_key, name, total_count, cluster_count, is_key, last_seen FROM devices ${where} ORDER BY cluster_count DESC, id DESC LIMIT ? OFFSET ?`, [...params, sz, (pg - 1) * sz]);
  const [c] = await p.query(`SELECT COUNT(*) AS total FROM devices ${where}`, params);
  res.json({ total: c[0].total, page: pg, pageSize: sz, items: rows });
});

router.get('/:id', async (req, res) => {
  const p = getPool();
  const [rows] = await p.query('SELECT * FROM devices WHERE id = ?', [req.params.id]);
  if (!rows.length) return res.status(404).json({ error: 'not found' });
  const [clusters] = await p.query('SELECT id, center_lat, center_lng, point_count, first_seen, last_seen FROM device_clusters WHERE device_id = ? ORDER BY point_count DESC', [req.params.id]);
  res.json({ device: rows[0], clusters });
});

router.get('/:id/trajectory', async (req, res) => {
  const p = getPool();
  const [rows] = await p.query('SELECT lat, lng, `signal`, seen_at FROM sightings WHERE device_id = ? ORDER BY seen_at DESC LIMIT 1000', [req.params.id]);
  const points = rows.map((r) => { const g = toGcj02IfChina(r.lat, r.lng); return { lat: g.lat, lng: g.lng, signal: r.signal, seen_at: r.seen_at }; });
  res.json({ points: points.reverse() });
});

router.get('/:id/sightings', async (req, res) => {
  const p = getPool();
  const pg = Math.max(1, parseInt(req.query.page, 10) || 1);
  const sz = Math.min(200, Math.max(1, parseInt(req.query.pageSize, 10) || 20));
  const [rows] = await p.query('SELECT id, uploader_id, lat, lng, `signal`, seen_at FROM sightings WHERE device_id = ? ORDER BY seen_at DESC LIMIT ? OFFSET ?', [req.params.id, sz, (pg - 1) * sz]);
  const [c] = await p.query('SELECT COUNT(*) AS total FROM sightings WHERE device_id = ?', [req.params.id]);
  res.json({ total: c[0].total, page: pg, pageSize: sz, items: rows });
});

module.exports = { router };
