const express = require('express');
const { getPool } = require('../db');
const { authMiddleware } = require('../auth');

const router = express.Router();
router.use(authMiddleware);

router.get('/overview', async (req, res) => {
  const p = getPool();
  const [[dev]] = await p.query('SELECT COUNT(*) AS c FROM devices');
  const [[sight]] = await p.query('SELECT COUNT(*) AS c FROM sightings');
  const [[key]] = await p.query('SELECT COUNT(*) AS c FROM devices WHERE is_key = 1');
  const [[today]] = await p.query("SELECT COUNT(*) AS c FROM sightings WHERE seen_at >= CURDATE()");
  res.json({ devices: dev.c, sightings: sight.c, keyDevices: key.c, todaySightings: today.c });
});

module.exports = { router };
