const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
const { config } = require('./config');

let pool = null;

function getPool() {
  if (!pool) {
    pool = mysql.createPool({ ...config.db, multipleStatements: true });
  }
  return pool;
}

async function initSchema() {
  const p = getPool();
  const sqlPath = path.join(__dirname, '..', 'sql', 'schema.sql');
  const sql = fs.readFileSync(sqlPath, 'utf8');
  await p.query(sql);
}

module.exports = { getPool, initSchema };
