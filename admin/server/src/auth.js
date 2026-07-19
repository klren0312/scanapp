const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { getPool } = require('./db');
const { config } = require('./config');

const router = express.Router();

function signToken(username) {
  return jwt.sign({ username }, config.jwtSecret, { expiresIn: `${config.jwtExpiresHours}h` });
}

router.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body || {};
    if (!username || !password) return res.status(400).json({ error: 'username and password required' });
    const p = getPool();
    const [rows] = await p.query('SELECT id, password_hash FROM admins WHERE username = ?', [username]);
    if (rows.length === 0) return res.status(401).json({ error: 'invalid credentials' });
    const ok = await bcrypt.compare(password, rows[0].password_hash);
    if (!ok) return res.status(401).json({ error: 'invalid credentials' });
    res.json({ token: signToken(username) });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: 'internal error' });
  }
});

function authMiddleware(req, res, next) {
  const h = req.headers.authorization || '';
  const token = h.startsWith('Bearer ') ? h.slice(7) : '';
  if (!token) return res.status(401).json({ error: 'missing token' });
  try {
    req.user = jwt.verify(token, config.jwtSecret);
    next();
  } catch (e) {
    res.status(401).json({ error: 'invalid token' });
  }
}

module.exports = { router, authMiddleware, signToken };
