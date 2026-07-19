const bcrypt = require('bcryptjs');
const { getPool } = require('./db');
const { config } = require('./config');

async function runSeed() {
  const p = getPool();
  const [rows] = await p.query('SELECT id FROM admins WHERE username = ?', [config.adminUsername]);
  if (rows.length > 0) return;
  const hash = await bcrypt.hash(config.adminPassword, 10);
  await p.query('INSERT INTO admins (username, password_hash) VALUES (?, ?)', [config.adminUsername, hash]);
  console.log(`[seed] created admin "${config.adminUsername}"`);
}

module.exports = { runSeed };
