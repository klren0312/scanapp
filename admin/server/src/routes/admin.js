const express = require('express');
const { authMiddleware } = require('../auth');
const { recomputeAll } = require('../cluster');
const { config } = require('../config');

const router = express.Router();
router.use(authMiddleware);

router.post('/recompute', async (req, res) => {
  try {
    await recomputeAll(config.clusterRadiusM);
    res.json({ ok: true });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: 'recompute failed' });
  }
});

module.exports = { router };
