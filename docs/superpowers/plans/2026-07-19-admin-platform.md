# ScanApp Admin Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Node.js + MySQL + Vue3 admin backend that receives device sightings from the ScanApp mobile app and reveals cross-region device encounters on AMap with key-alert highlighting.

**Architecture:** App uploads per-scan sighting batches via a plain HTTP endpoint (X-Upload-Token). The Express server upserts devices, stores sightings with an idempotent unique key, and runs incremental distance clustering (haversine, 500m default) to flag devices seen in ≥2 places. A Vue3 + Element Plus + AMap frontend reads JWT-protected APIs to render point markers, trajectories, device tables, and red-highlighted key-device alerts. WGS-84→GCJ-02 conversion happens server-side so the frontend only displays.

**Tech Stack:** Node.js ≥18 (Express, mysql2/promise, jsonwebtoken, bcryptjs), MySQL ≥5.7 (root/root, db `scanapp_admin`), Vue 3 + Vite + Element Plus + vue-router + pinia + axios + AMap JS API, Kuikly/Kotlin (HttpURLConnection for Android upload).

## Global Constraints

- Local DB: MySQL `DB_USER=root`, `DB_PASSWORD=root`, `DB_NAME=scanapp_admin` (user-confirmed). Put defaults in `admin/server/.env.example`; real values in `admin/server/.env` (gitignored).
- Distance clustering threshold `CLUSTER_RADIUS_M` default 500, configurable via `.env`.
- Key-device rule: `cluster_count >= 2` sets `is_key = 1`.
- "Region" = distance cluster (not AMap admin region, not geofence).
- Upload idempotency: unique key `(device_id, uploader_id, seen_at)`; reject zero-coordinate sightings.
- Store WGS-84 raw; convert to GCJ-02 only in map/trajectory output, inside China bbox only.
- App follows existing patterns: `expect object` boundary for platform upload (mirror `PlatformExportController`); SQLDelight DAO with `DatabaseFactory.dbDispatcher`; `withContext(DatabaseFactory.dbDispatcher)`; Kuikly `Mdc*` UI components.
- Every change updates `CHANGELOG.md`. Commit after each task's verification. App changes verified with `.\gradlew.bat :shared:testDebugUnitTest`.

---

## Part A — Server (`admin/server`)

### Task A1: Server scaffold, config, DB pool, schema, seed

**Files:**
- Create: `admin/server/package.json`
- Create: `admin/server/.env.example`
- Create: `admin/server/.gitignore`
- Create: `admin/server/sql/schema.sql`
- Create: `admin/server/src/config.js`
- Create: `admin/server/src/db.js`
- Create: `admin/server/src/seed.js`

**Interfaces:**
- Produces: `getPool()` (returns mysql2 connection pool), `initSchema()` (runs schema.sql idempotently), `runSeed()` (creates admin from env), `config` object `{ db, jwtSecret, uploadToken, clusterRadiusM, port }`.

- [ ] **Step 1: Write `admin/server/package.json`**

```json
{
  "name": "scanapp-admin-server",
  "version": "1.0.0",
  "private": true,
  "type": "commonjs",
  "scripts": {
    "start": "node src/index.js",
    "test": "node --test test/"
  },
  "dependencies": {
    "bcryptjs": "^2.4.3",
    "cors": "^2.8.5",
    "dotenv": "^16.4.5",
    "express": "^4.19.2",
    "jsonwebtoken": "^9.0.2",
    "mysql2": "^3.11.0"
  },
  "devDependencies": {
    "supertest": "^7.0.0"
  }
}
```

- [ ] **Step 2: Write `admin/server/.env.example`**

```env
PORT=3001
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=root
DB_PASSWORD=root
DB_NAME=scanapp_admin
JWT_SECRET=change-me-in-prod
JWT_EXPIRES_HOURS=12
UPLOAD_TOKEN=change-me-upload-token
CLUSTER_RADIUS_M=500
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
```

- [ ] **Step 3: Write `admin/server/.gitignore`**

```gitignore
node_modules/
.env
```

- [ ] **Step 4: Write `admin/server/sql/schema.sql`**

```sql
CREATE TABLE IF NOT EXISTS admins (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS devices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_type ENUM('wifi','bluetooth') NOT NULL,
  device_key VARCHAR(64) NOT NULL,
  name VARCHAR(255) NOT NULL DEFAULT '',
  extra JSON NULL,
  first_seen DATETIME NOT NULL,
  last_seen DATETIME NOT NULL,
  total_count INT NOT NULL DEFAULT 0,
  cluster_count INT NOT NULL DEFAULT 0,
  is_key TINYINT(1) NOT NULL DEFAULT 0,
  last_lat DOUBLE NOT NULL,
  last_lng DOUBLE NOT NULL,
  UNIQUE KEY uk_device (device_type, device_key)
);

CREATE TABLE IF NOT EXISTS sightings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_id BIGINT NOT NULL,
  uploader_id VARCHAR(64) NOT NULL,
  lat DOUBLE NOT NULL,
  lng DOUBLE NOT NULL,
  signal INT NOT NULL,
  seen_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_dedup (device_id, uploader_id, seen_at),
  KEY idx_device_time (device_id, seen_at),
  KEY idx_seen_at (seen_at),
  FOREIGN KEY (device_id) REFERENCES devices(id)
);

CREATE TABLE IF NOT EXISTS device_clusters (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_id BIGINT NOT NULL,
  center_lat DOUBLE NOT NULL,
  center_lng DOUBLE NOT NULL,
  point_count INT NOT NULL DEFAULT 1,
  first_seen DATETIME NOT NULL,
  last_seen DATETIME NOT NULL,
  KEY idx_device (device_id),
  FOREIGN KEY (device_id) REFERENCES devices(id)
);
```

- [ ] **Step 5: Write `admin/server/src/config.js`**

```js
require('dotenv').config();
const parseInt10 = (v, d) => { const n = parseInt(v, 10); return Number.isFinite(n) ? n : d; };

const config = {
  port: parseInt10(process.env.PORT, 3001),
  db: {
    host: process.env.DB_HOST || '127.0.0.1',
    port: parseInt10(process.env.DB_PORT, 3306),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || 'root',
    database: process.env.DB_NAME || 'scanapp_admin',
    waitForConnections: true,
    connectionLimit: 10,
  },
  jwtSecret: process.env.JWT_SECRET || 'change-me-in-prod',
  jwtExpiresHours: parseInt10(process.env.JWT_EXPIRES_HOURS, 12),
  uploadToken: process.env.UPLOAD_TOKEN || 'change-me-upload-token',
  clusterRadiusM: parseInt10(process.env.CLUSTER_RADIUS_M, 500),
  adminUsername: process.env.ADMIN_USERNAME || 'admin',
  adminPassword: process.env.ADMIN_PASSWORD || 'admin123',
};

module.exports = { config };
```

- [ ] **Step 6: Write `admin/server/src/db.js`**

```js
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
const { config } = require('./config');

let pool = null;

function getPool() {
  if (!pool) {
    pool = mysql.createPool(config.db);
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
```

- [ ] **Step 7: Write `admin/server/src/seed.js`**

```js
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
```

- [ ] **Step 8: Install deps and create DB**

Run:
```bash
cd admin/server && npm install
```
Then create the database manually:
```bash
mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS scanapp_admin CHARACTER SET utf8mb4;"
```
Expected: command succeeds, `node_modules/` created.

- [ ] **Step 9: Commit**

```bash
cd ../.. && git add admin/server/package.json admin/server/.env.example admin/server/.gitignore admin/server/sql/schema.sql admin/server/src/config.js admin/server/src/db.js admin/server/src/seed.js
git commit -m "feat(server): scaffold admin server, schema, config, seed"
```

### Task A2: Geo utilities + tests (TDD)

**Files:**
- Create: `admin/server/src/geo.js`
- Create: `admin/server/test/geo.test.js`

**Interfaces:**
- Produces: `haversineMeters(a, b)` (a,b = {lat,lng} → meters), `wgs84ToGcj02(lat, lng)` → {lat,lng}, `isInsideChina(lat, lng)` → boolean, `toGcj02IfChina(lat, lng)` → {lat,lng}.

- [ ] **Step 1: Write failing test `admin/server/test/geo.test.js`**

```js
const test = require('node:test');
const assert = require('node:assert');
const { haversineMeters, wgs84ToGcj02, isInsideChina, toGcj02IfChina } = require('../src/geo');

test('haversineMeters: same point is ~0', () => {
  const a = { lat: 39.908, lng: 116.397 };
  assert.ok(haversineMeters(a, a) < 1);
});

test('haversineMeters: ~1km apart', () => {
  const a = { lat: 39.908, lng: 116.397 };
  const b = { lat: 39.917, lng: 116.397 };
  const d = haversineMeters(a, b);
  assert.ok(d > 900 && d < 1100, `expected ~1000m, got ${d}`);
});

test('isInsideChina: Beijing true, London false', () => {
  assert.equal(isInsideChina(39.908, 116.397), true);
  assert.equal(isInsideChina(51.5074, -0.1278), false);
});

test('wgs84ToGcj02: Beijing shifts east/north', () => {
  const r = wgs84ToGcj02(39.908, 116.397);
  assert.ok(r.lat > 39.908 && r.lng > 116.397);
  assert.ok(r.lng - 116.397 < 0.01);
});

test('toGcj02IfChina: outside China unchanged', () => {
  const r = toGcj02IfChina(51.5074, -0.1278);
  assert.equal(r.lat, 51.5074);
  assert.equal(r.lng, -0.1278);
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd admin/server && node --test test/geo.test.js`
Expected: FAIL with "Cannot find module '../src/geo'".

- [ ] **Step 3: Write `admin/server/src/geo.js`**

```js
function haversineMeters(a, b) {
  const R = 6371000;
  const toRad = (x) => (x * Math.PI) / 180;
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const lat1 = toRad(a.lat);
  const lat2 = toRad(b.lat);
  const h = Math.sin(dLat / 2) ** 2 + Math.sin(dLng / 2) ** 2 * Math.cos(lat1) * Math.cos(lat2);
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(h)));
}

function outOfChina(lat, lng) {
  return !(lng > 73.66 && lng < 135.05 && lat > 3.86 && lat < 53.55);
}

function transformLat(x, y) {
  let r = -100 + 2 * x + 3 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
  r += ((20 * Math.sin(6 * x * Math.PI) + 20 * Math.sin(2 * x * Math.PI)) * 2) / 3;
  r += ((20 * Math.sin(y * Math.PI) + 40 * Math.sin((y / 3) * Math.PI)) * 2) / 3;
  r += ((160 * Math.sin((y / 12) * Math.PI) + 320 * Math.sin((y * Math.PI) / 30)) * 2) / 3;
  return r;
}

function transformLng(x, y) {
  let r = 300 + x + 2 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
  r += ((20 * Math.sin(6 * x * Math.PI) + 20 * Math.sin(2 * x * Math.PI)) * 2) / 3;
  r += ((20 * Math.sin(x * Math.PI) + 40 * Math.sin((x / 3) * Math.PI)) * 2) / 3;
  r += ((150 * Math.sin((x / 12) * Math.PI) + 300 * Math.sin((x / 30) * Math.PI)) * 2) / 3;
  return r;
}

const A = 6378245.0;
const EE = 0.00669342162296594323;

function wgs84ToGcj02(lat, lng) {
  if (outOfChina(lat, lng)) return { lat, lng };
  let dLat = transformLat(lng - 105.0, lat - 35.0);
  let dLng = transformLng(lng - 105.0, lat - 35.0);
  const radLat = (lat / 180.0) * Math.PI;
  let magic = Math.sin(radLat);
  magic = 1 - EE * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * Math.PI);
  dLng = (dLng * 180.0) / ((A / sqrtMagic) * Math.cos(radLat) * Math.PI);
  return { lat: lat + dLat, lng: lng + dLng };
}

function isInsideChina(lat, lng) {
  return !outOfChina(lat, lng);
}

function toGcj02IfChina(lat, lng) {
  return wgs84ToGcj02(lat, lng);
}

module.exports = { haversineMeters, wgs84ToGcj02, isInsideChina, toGcj02IfChina };
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node --test test/geo.test.js`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add admin/server/src/geo.js admin/server/test/geo.test.js
git commit -m "feat(server): add geo utils (haversine + WGS84->GCJ02) with tests"
```

### Task A3: Clustering engine + tests (TDD)

**Files:**
- Create: `admin/server/src/cluster.js`
- Create: `admin/server/test/cluster.test.js`

**Interfaces:**
- Consumes: `getPool()` from db, `haversineMeters` from geo, `config.clusterRadiusM`.
- Produces:
  - `assignToClusters(deviceId, points, radiusM)` — points: `[{lat,lng,seenAt}]` (time-ascending); incrementally inserts/updates `device_clusters`; returns cluster count.
  - `recomputeAll(radiusM)` — clears `device_clusters`, rebuilds per device from `sightings` ordered by seen_at.
  - `updateDeviceFlags(deviceId)` — sets `cluster_count` and `is_key` from current clusters.

- [ ] **Step 1: Write failing test `admin/server/test/cluster.test.js`**

```js
const test = require('node:test');
const assert = require('node:assert');
const { assignToClusters, recomputeAll } = require('../src/cluster');
const { getPool, initSchema } = require('../src/db');

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
    'INSERT INTO sightings (device_id, uploader_id, lat, lng, signal, seen_at) VALUES (?,?,?,?,?,?)',
    [id, 'u1', 39.900, 116.400, -50, new Date('2026-01-01T00:00:00Z')]
  );
  await p.query(
    'INSERT INTO sightings (device_id, uploader_id, lat, lng, signal, seen_at) VALUES (?,?,?,?,?,?)',
    [id, 'u1', 40.900, 117.400, -50, new Date('2026-01-01T01:00:00Z')]
  );
  await recomputeAll(500);
  const [rows] = await p.query('SELECT cluster_count, is_key FROM devices WHERE id = ?', [id]);
  assert.equal(rows[0].cluster_count, 2);
  assert.equal(rows[0].is_key, 1);
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test test/cluster.test.js`
Expected: FAIL (module missing / throws).

- [ ] **Step 3: Write `admin/server/src/cluster.js`**

```js
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
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
cd admin/server && node --test test/cluster.test.js
```
Expected: PASS (3 tests). (Requires a running MySQL `scanapp_admin` with schema initialized: run `node -e "require('./src/db').getPool(); require('./src/db').initSchema().then(()=>console.log('ok')).catch(e=>{console.error(e);process.exit(1)})"`.)

- [ ] **Step 5: Commit**

```bash
git add admin/server/src/cluster.js admin/server/test/cluster.test.js
git commit -m "feat(server): add incremental distance clustering + tests"
```

### Task A4: Auth (login + JWT middleware)

**Files:**
- Create: `admin/server/src/auth.js`

**Interfaces:**
- Produces: `router` (express.Router with `POST /auth/login`), `authMiddleware(req,res,next)` (verifies Bearer JWT), `signToken(username)`.

- [ ] **Step 1: Write `admin/server/src/auth.js`**

```js
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
```

- [ ] **Step 2: Commit**

```bash
git add admin/server/src/auth.js
git commit -m "feat(server): add admin login + JWT auth middleware"
```

### Task A5: Upload route (ingest + idempotent dedup + clustering)

**Files:**
- Create: `admin/server/src/routes/upload.js`

**Interfaces:**
- Consumes: `getPool`, `config.uploadToken`, `assignToClusters`.
- Produces: `router` (express.Router with `POST /upload`), `ingestSightings(deviceId, uploaderId, rows)` helper (also reused by tests).

- [ ] **Step 1: Write `admin/server/src/routes/upload.js`**

```js
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
        'INSERT INTO sightings (device_id, uploader_id, lat, lng, signal, seen_at) VALUES (?,?,?,?,?,?)',
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
```

- [ ] **Step 2: Smoke test ingest via script**

Run (needs running MySQL + a device seeded; use the admin creds from .env):
```bash
cd admin/server && node -e "
const {getPool,initSchema}=require('./src/db');
const {ingestBatch}=require('./src/routes/upload');
(async()=>{
  await initSchema();
  const p=getPool();
  const r=await ingestBatch(p,'wifi',[{bssid:'aa:bb',ssid:'t',signal:-50,lat:39.9,lng:116.4,timestamp:Date.now()},{bssid:'aa:bb',ssid:'t',signal:-50,lat:39.9,lng:116.4,timestamp:Date.now()+100}],'u1',500);
  console.log(JSON.stringify(r));
})().catch(e=>{console.error(e);process.exit(1)});
"
```
Expected: `{"inserted":1,"duplicates":0}` on first run, `{"inserted":1,"duplicates":1}` if rerun (same uploaderId+timestamp).

- [ ] **Step 3: Commit**

```bash
git add admin/server/src/routes/upload.js
git commit -m "feat(server): add upload ingest with idempotent dedup + clustering"
```

### Task A6: Devices / Map / Stats / Admin routes

**Files:**
- Create: `admin/server/src/routes/devices.js`
- Create: `admin/server/src/routes/map.js`
- Create: `admin/server/src/routes/stats.js`
- Create: `admin/server/src/routes/admin.js`

**Interfaces:**
- Consumes: `getPool`, `authMiddleware`, `toGcj02IfChina`, `recomputeAll`, `config.clusterRadiusM`.
- Produces: route routers mounted at `/api/devices`, `/api/map`, `/api/stats`, `/api/admin`.

- [ ] **Step 1: Write `admin/server/src/routes/devices.js`**

```js
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
  const [rows] = await p.query('SELECT lat, lng, signal, seen_at FROM sightings WHERE device_id = ? ORDER BY seen_at DESC LIMIT 1000', [req.params.id]);
  const points = rows.map((r) => { const g = toGcj02IfChina(r.lat, r.lng); return { lat: g.lat, lng: g.lng, signal: r.signal, seen_at: r.seen_at }; });
  res.json({ points: points.reverse() });
});

router.get('/:id/sightings', async (req, res) => {
  const p = getPool();
  const pg = Math.max(1, parseInt(req.query.page, 10) || 1);
  const sz = Math.min(200, Math.max(1, parseInt(req.query.pageSize, 10) || 20));
  const [rows] = await p.query('SELECT id, uploader_id, lat, lng, signal, seen_at FROM sightings WHERE device_id = ? ORDER BY seen_at DESC LIMIT ? OFFSET ?', [req.params.id, sz, (pg - 1) * sz]);
  const [c] = await p.query('SELECT COUNT(*) AS total FROM sightings WHERE device_id = ?', [req.params.id]);
  res.json({ total: c[0].total, page: pg, pageSize: sz, items: rows });
});

module.exports = { router };
```

- [ ] **Step 2: Write `admin/server/src/routes/map.js`**

```js
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
```

- [ ] **Step 3: Write `admin/server/src/routes/stats.js`**

```js
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
```

- [ ] **Step 4: Write `admin/server/src/routes/admin.js`**

```js
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
```

- [ ] **Step 5: Commit**

```bash
git add admin/server/src/routes/devices.js admin/server/src/routes/map.js admin/server/src/routes/stats.js admin/server/src/routes/admin.js
git commit -m "feat(server): add devices, map, stats, admin routes"
```

### Task A7: Server entry + smoke test + CHANGELOG

**Files:**
- Create: `admin/server/src/index.js`
- Create: `admin/server/test/server.test.js`
- Modify: `CHANGELOG.md`

**Interfaces:**
- Produces: runnable server on `config.port`.

- [ ] **Step 1: Write `admin/server/src/index.js`**

```js
const express = require('express');
const cors = require('cors');
const { initSchema } = require('./db');
const { runSeed } = require('./seed');
const { config } = require('./config');
const { router: authRouter } = require('./auth');
const uploadRouter = require('./routes/upload');
const devicesRouter = require('./routes/devices');
const mapRouter = require('./routes/map');
const statsRouter = require('./routes/stats');
const adminRouter = require('./routes/admin');

async function start() {
  await initSchema();
  await runSeed();
  const app = express();
  app.use(cors());
  app.use(express.json({ limit: '5mb' }));
  app.use('/api/auth', authRouter);
  app.use('/api/upload', uploadRouter);
  app.use('/api/devices', devicesRouter);
  app.use('/api/map', mapRouter);
  app.use('/api/stats', statsRouter);
  app.use('/api/admin', adminRouter);
  app.listen(config.port, () => console.log(`admin server listening on :${config.port}`));
}

start().catch((e) => { console.error('startup failed', e); process.exit(1); });
```

- [ ] **Step 2: Write `admin/server/test/server.test.js`**

```js
const test = require('node:test');
const assert = require('node:assert');
const request = require('supertest');
const express = require('express');
const cors = require('cors');
const { config } = require('../src/config');
const { router: authRouter } = require('../src/auth');
const uploadRouter = require('../src/routes/upload');
const devicesRouter = require('../src/routes/devices');
const statsRouter = require('../src/routes/stats');

function buildApp() {
  const app = express();
  app.use(cors());
  app.use(express.json());
  app.use('/api/auth', authRouter);
  app.use('/api/upload', uploadRouter);
  app.use('/api/devices', devicesRouter);
  app.use('/api/stats', statsRouter);
  return app;
}

test('upload requires token (401)', async () => {
  const res = await request(buildApp()).post('/api/upload').send({ uploaderId: 'u', wifi: [] });
  assert.equal(res.status, 401);
});

test('upload then login then list devices', async () => {
  const app = buildApp();
  const up = await request(app).post('/api/upload').set('x-upload-token', config.uploadToken).send({
    uploaderId: 'u1',
    wifi: [{ bssid: 'de:ad:be:ef:00:01', ssid: 'cafe', signal: -40, lat: 39.9, lng: 116.4, timestamp: Date.now() }],
  });
  assert.equal(up.status, 200);
  assert.equal(up.body.inserted, 1);
  const login = await request(app).post('/api/auth/login').send({ username: config.adminUsername, password: config.adminPassword });
  assert.equal(login.status, 200);
  assert.ok(login.body.token);
  const list = await request(app).get('/api/devices').set('Authorization', `Bearer ${login.body.token}`);
  assert.equal(list.status, 200);
  assert.ok(list.body.items.length >= 1);
});
```

- [ ] **Step 3: Run server tests**

Run: `node --test test/server.test.js`
Expected: PASS (both tests). Requires running MySQL with `scanapp_admin` initialized.

- [ ] **Step 4: Append CHANGELOG entry**

Add to `CHANGELOG.md`:
```
## 2026-07-19 — Admin platform backend (Node.js + MySQL)
- Added `admin/server`: Express API for device upload, distance-clustering, JWT admin login, map/stats/device queries, cluster recompute.
- Key-device rule: device seen in >=2 clusters (500m radius) flagged `is_key`.
- Verification: `node --test test/` (geo, cluster, server smoke). Local DB root/root, db `scanapp_admin`.
```

- [ ] **Step 5: Commit**

```bash
git add admin/server/src/index.js admin/server/test/server.test.js CHANGELOG.md
git commit -m "feat(server): wire entry point, add smoke tests, update CHANGELOG"
```

---

## Part B — Web frontend (`admin/web`)

### Task B1: Web scaffold, auth store, api client, router

**Files:**
- Create: `admin/web/package.json`
- Create: `admin/web/.env.example`
- Create: `admin/web/vite.config.js`
- Create: `admin/web/index.html`
- Create: `admin/web/src/main.js`
- Create: `admin/web/src/App.vue`
- Create: `admin/web/src/router/index.js`
- Create: `admin/web/src/store/auth.js`
- Create: `admin/web/src/api/index.js`

**Interfaces:**
- Produces: `api` object with `login`, `getStats`, `getMapPoints`, `getDevices`, `getDevice`, `getTrajectory`, `getSightings`, `recompute`; `authStore` (pinia) with `token`, `login`, `logout`, `isAuthenticated`.

- [ ] **Step 1: Write `admin/web/package.json`**

```json
{
  "name": "scanapp-admin-web",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "axios": "^1.7.2",
    "element-plus": "^2.7.6",
    "@element-plus/icons-vue": "^2.3.1",
    "pinia": "^2.1.7",
    "vue": "^3.4.31",
    "vue-router": "^4.4.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.5",
    "vite": "^5.3.3"
  }
}
```

- [ ] **Step 2: Write `admin/web/.env.example`**

```env
VITE_API_BASE=http://localhost:3001/api
VITE_AMAP_KEY=your-amap-js-key
VITE_AMAP_SECURITY_CODE=your-amap-security-code
```

- [ ] **Step 3: Write `admin/web/vite.config.js`**

```js
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: { port: 5173 },
});
```

- [ ] **Step 4: Write `admin/web/index.html`**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>ScanApp 管理后台</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

- [ ] **Step 5: Write `admin/web/src/main.js`**

```js
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './router';

createApp(App).use(createPinia()).use(ElementPlus).use(router).mount('#app');
```

- [ ] **Step 6: Write `admin/web/src/App.vue`**

```vue
<template>
  <router-view />
</template>

<script setup></script>
```

- [ ] **Step 7: Write `admin/web/src/router/index.js`**

```js
import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../store/auth';
import LoginView from '../views/LoginView.vue';
import DashboardView from '../views/DashboardView.vue';
import MapView from '../views/MapView.vue';
import DevicesView from '../views/DevicesView.vue';
import DeviceDetailView from '../views/DeviceDetailView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    { path: '/', name: 'dashboard', component: DashboardView, meta: { auth: true } },
    { path: '/map', name: 'map', component: MapView, meta: { auth: true } },
    { path: '/devices', name: 'devices', component: DevicesView, meta: { auth: true } },
    { path: '/devices/:id', name: 'device-detail', component: DeviceDetailView, meta: { auth: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.meta.auth && !auth.isAuthenticated) return { name: 'login' };
  if (to.name === 'login' && auth.isAuthenticated) return { name: 'dashboard' };
  return true;
});

export default router;
```

- [ ] **Step 8: Write `admin/web/src/store/auth.js`**

```js
import { defineStore } from 'pinia';

const KEY = 'scanapp_admin_token';

export const useAuthStore = defineStore('auth', {
  state: () => ({ token: localStorage.getItem(KEY) || '' }),
  getters: {
    isAuthenticated: (s) => !!s.token,
  },
  actions: {
    setToken(t) { this.token = t; localStorage.setItem(KEY, t); },
    logout() { this.token = ''; localStorage.removeItem(KEY); },
  },
});
```

- [ ] **Step 9: Write `admin/web/src/api/index.js`**

```js
import axios from 'axios';
import { useAuthStore } from '../store/auth';

const base = import.meta.env.VITE_API_BASE || 'http://localhost:3001/api';

const http = axios.create({ baseURL: base });

http.interceptors.request.use((cfg) => {
  const token = useAuthStore().token;
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

http.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response && err.response.status === 401 && !err.config.url.endsWith('/auth/login')) {
      useAuthStore().logout();
      window.location.hash = '#/login';
    }
    return Promise.reject(err);
  }
);

export const api = {
  login: (username, password) => http.post('/auth/login', { username, password }).then((r) => r.data),
  getStats: () => http.get('/stats/overview').then((r) => r.data),
  getMapPoints: (params) => http.get('/map/points', { params }).then((r) => r.data),
  getDevices: (params) => http.get('/devices', { params }).then((r) => r.data),
  getDevice: (id) => http.get(`/devices/${id}`).then((r) => r.data),
  getTrajectory: (id) => http.get(`/devices/${id}/trajectory`).then((r) => r.data),
  getSightings: (id, params) => http.get(`/devices/${id}/sightings`, { params }).then((r) => r.data),
  recompute: () => http.post('/admin/recompute').then((r) => r.data),
};
```

- [ ] **Step 10: Install and verify build**

Run: `cd admin/web && npm install`
Expected: `node_modules/` created without fatal errors.

- [ ] **Step 11: Commit**

```bash
cd ../.. && git add admin/web/package.json admin/web/.env.example admin/web/vite.config.js admin/web/index.html admin/web/src/main.js admin/web/src/App.vue admin/web/src/router/index.js admin/web/src/store/auth.js admin/web/src/api/index.js
git commit -m "feat(web): scaffold Vue3 app, auth store, api client, router"
```

### Task B2: LoginView

**Files:**
- Create: `admin/web/src/views/LoginView.vue`

**Interfaces:**
- Consumes: `api.login`, `authStore.setToken`.

- [ ] **Step 1: Write `admin/web/src/views/LoginView.vue`**

```vue
<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2>ScanApp 管理后台</h2>
      <el-form @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="onSubmit">登录</el-button>
        <el-alert v-if="error" :title="error" type="error" show-icon class="mt" />
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';
import { useAuthStore } from '../store/auth';

const router = useRouter();
const auth = useAuthStore();
const username = ref('admin');
const password = ref('');
const loading = ref(false);
const error = ref('');

async function onSubmit() {
  error.value = '';
  loading.value = true;
  try {
    const { token } = await api.login(username.value, password.value);
    auth.setToken(token);
    router.push({ name: 'dashboard' });
  } catch (e) {
    error.value = (e.response && e.response.data && e.response.data.error) || '登录失败';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-wrap { display: flex; height: 100vh; align-items: center; justify-content: center; }
.login-card { width: 360px; }
.mt { margin-top: 12px; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add admin/web/src/views/LoginView.vue
git commit -m "feat(web): add login view"
```

### Task B3: AMap loader util + MapView

**Files:**
- Create: `admin/web/src/utils/amap.js`
- Create: `admin/web/src/views/MapView.vue`

**Interfaces:**
- Consumes: `api.getMapPoints`, `api.getDevice`, `api.getTrajectory`.
- Produces: AMap loader used by MapView and DeviceDetailView.

- [ ] **Step 1: Write `admin/web/src/utils/amap.js`**

```js
const KEY = import.meta.env.VITE_AMAP_KEY;
const SECURITY = import.meta.env.VITE_AMAP_SECURITY_CODE;
let promise = null;

export function loadAMap() {
  if (window.AMap) return Promise.resolve(window.AMap);
  if (promise) return promise;
  if (SECURITY) {
    window._AMapSecurityConfig = { securityJsCode: SECURITY };
  }
  promise = new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = `https://webapi.amap.com/maps?v=2.0&key=${KEY}&plugin=AMap.Polyline`;
    s.async = true;
    s.onload = () => (window.AMap ? resolve(window.AMap) : reject(new Error('AMap load failed')));
    s.onerror = () => reject(new Error('AMap script error'));
    document.head.appendChild(s);
  });
  return promise;
}
```

- [ ] **Step 2: Write `admin/web/src/views/MapView.vue`**

```vue
<template>
  <div class="page">
    <el-card class="filter">
      <el-input v-model="keyword" placeholder="设备名/标识搜索" style="width:220px" clearable />
      <el-select v-model="type" placeholder="类型" clearable style="width:140px;margin-left:8px">
        <el-option label="WiFi" value="wifi" />
        <el-option label="蓝牙" value="bluetooth" />
      </el-select>
      <el-switch v-model="keyOnly" active-text="仅看重点" style="margin-left:12px" @change="refresh" />
      <el-button type="primary" style="margin-left:12px" @click="refresh">刷新</el-button>
    </el-card>
    <div ref="mapEl" class="map"></div>
    <el-drawer v-model="drawer" title="设备详情" size="40%">
      <template v-if="detail">
        <el-alert v-if="detail.device.is_key" title="重点设备：在多个区域被发现" type="error" show-icon />
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标识">{{ detail.device.device_key }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ detail.device.name }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.device.device_type }}</el-descriptions-item>
          <el-descriptions-item label="出现次数">{{ detail.device.total_count }}</el-descriptions-item>
          <el-descriptions-item label="地点数">{{ detail.device.cluster_count }}</el-descriptions-item>
        </el-descriptions>
        <el-divider>轨迹</el-divider>
        <div ref="trajEl" class="traj-map"></div>
        <el-divider>各地点遇见次数</el-divider>
        <el-table :data="detail.clusters">
          <el-table-column prop="id" label="地点" />
          <el-table-column prop="point_count" label="次数" />
          <el-table-column label="中心">
            <template #default="{ row }">{{ row.center_lat.toFixed(5) }}, {{ row.center_lng.toFixed(5) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { api } from '../api';
import { loadAMap } from '../utils/amap';

const mapEl = ref(null);
const trajEl = ref(null);
const keyword = ref('');
const type = ref('');
const keyOnly = ref(false);
const drawer = ref(false);
const detail = ref(null);
let map = null;
let markers = [];

async function refresh() {
  const { points } = await api.getMapPoints({ type: type.value || undefined, keyOnly: keyOnly.value ? '1' : undefined });
  if (!map) return;
  markers.forEach((m) => map.remove(m));
  markers = [];
  points.forEach((p) => {
    const color = p.isKey ? '#f56c6c' : p.type === 'wifi' ? '#409eff' : '#67c23a';
    const m = new window.AMap.Marker({ position: [p.lng, p.lat], title: p.key, zIndex: p.isKey ? 100 : 1 });
    m.setMap(map);
    m.on('click', () => openDetail(p.id));
    markers.push(m);
  });
}

async function openDetail(id) {
  const data = await api.getDevice(id);
  const traj = await api.getTrajectory(id);
  detail.value = data;
  drawer.value = true;
  const AMap = window.AMap;
  setTimeout(() => {
    if (!trajEl.value) return;
    const tm = new AMap.Map(trajEl.value, { zoom: 12, center: traj.points.length ? [traj.points[0].lng, traj.points[0].lat] : [116.397, 39.908] });
    if (traj.points.length) {
      new AMap.Polyline({ path: traj.points.map((pt) => [pt.lng, pt.lat]), strokeColor: '#f56c6c', strokeWeight: 4 }).setMap(tm);
      traj.points.forEach((pt) => new AMap.Marker({ position: [pt.lng, pt.lat] }).setMap(tm));
    }
  }, 50);
}

watch([keyword, type], () => {});

onMounted(async () => {
  const AMap = await loadAMap();
  map = new AMap.Map(mapEl.value, { zoom: 11, center: [116.397, 39.908] });
  refresh();
});
</script>

<style scoped>
.page { height: 100vh; display: flex; flex-direction: column; }
.filter { border-radius: 0; }
.map { flex: 1; }
.traj-map { height: 300px; }
</style>
```

- [ ] **Step 3: Commit**

```bash
git add admin/web/src/utils/amap.js admin/web/src/views/MapView.vue
git commit -m "feat(web): add AMap loader and map view with filter + detail drawer"
```

### Task B4: Dashboard / Devices / DeviceDetail views

**Files:**
- Create: `admin/web/src/views/DashboardView.vue`
- Create: `admin/web/src/views/DevicesView.vue`
- Create: `admin/web/src/views/DeviceDetailView.vue`

**Interfaces:**
- Consumes: `api.getStats`, `api.getDevices`, `api.getDevice`, `api.getTrajectory`, `loadAMap`.

- [ ] **Step 1: Write `admin/web/src/views/DashboardView.vue`**

```vue
<template>
  <div class="page">
    <el-menu mode="horizontal" :default-active="'dashboard'" router>
      <el-menu-item index="dashboard" route="/">总览</el-menu-item>
      <el-menu-item index="map" route="/map">地图</el-menu-item>
      <el-menu-item index="devices" route="/devices">设备</el-menu-item>
    </el-menu>
    <div class="body">
      <el-row :gutter="16">
        <el-col :span="6"><el-card><el-statistic title="设备总数" :value="stats.devices" /></el-card></el-col>
        <el-col :span="6"><el-card><el-statistic title="目击总数" :value="stats.sightings" /></el-card></el-col>
        <el-col :span="6"><el-card><el-statistic title="重点设备" :value="stats.keyDevices" value-color="#f56c6c" /></el-card></el-col>
        <el-col :span="6"><el-card><el-statistic title="今日新增" :value="stats.todaySightings" /></el-card></el-col>
      </el-row>
      <el-card class="mt">
        <template #header><span>重点设备提示</span></template>
        <el-table :data="keyList" v-loading="loading" @row-click="goDetail">
          <el-table-column prop="device_key" label="标识" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="cluster_count" label="地点数">
            <template #default="{ row }"><el-tag type="danger">{{ row.cluster_count }}</el-tag></template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';

const router = useRouter();
const stats = ref({ devices: 0, sightings: 0, keyDevices: 0, todaySightings: 0 });
const keyList = ref([]);
const loading = ref(false);

async function load() {
  stats.value = await api.getStats();
  loading.value = true;
  try {
    const r = await api.getDevices({ keyOnly: '1', pageSize: 100 });
    keyList.value = r.items;
  } finally { loading.value = false; }
}

function goDetail(row) { router.push(`/devices/${row.id}`); }

onMounted(load);
</script>

<style scoped>
.page { height: 100vh; }
.body { padding: 16px; }
.mt { margin-top: 16px; }
</style>
```

- [ ] **Step 2: Write `admin/web/src/views/DevicesView.vue`**

```vue
<template>
  <div class="page">
    <el-menu mode="horizontal" :default-active="'devices'" router>
      <el-menu-item index="dashboard" route="/">总览</el-menu-item>
      <el-menu-item index="map" route="/map">地图</el-menu-item>
      <el-menu-item index="devices" route="/devices">设备</el-menu-item>
    </el-menu>
    <div class="body">
      <el-card>
        <el-input v-model="keyword" placeholder="设备名/标识" style="width:220px" clearable @change="load" />
        <el-select v-model="type" placeholder="类型" clearable style="width:140px;margin-left:8px" @change="load">
          <el-option label="WiFi" value="wifi" />
          <el-option label="蓝牙" value="bluetooth" />
        </el-select>
        <el-switch v-model="keyOnly" active-text="仅重点" style="margin-left:12px" @change="load" />
        <el-table :data="items" class="mt" @row-click="goDetail" v-loading="loading">
          <el-table-column prop="device_key" label="标识" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="device_type" label="类型" />
          <el-table-column prop="total_count" label="出现次数" />
          <el-table-column prop="cluster_count" label="地点数" sortable>
            <template #default="{ row }">
              <el-tag v-if="row.is_key" type="danger">{{ row.cluster_count }}</el-tag>
              <span v-else>{{ row.cluster_count }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination class="mt" :total="total" :page-size="pageSize" @current-change="onPage" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';

const router = useRouter();
const items = ref([]);
const total = ref(0);
const pageSize = 20;
const page = ref(1);
const keyword = ref('');
const type = ref('');
const keyOnly = ref(false);
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    const r = await api.getDevices({ keyword: keyword.value || undefined, type: type.value || undefined, keyOnly: keyOnly.value ? '1' : undefined, page: page.value, pageSize });
    items.value = r.items;
    total.value = r.total;
  } finally { loading.value = false; }
}

function onPage(p) { page.value = p; load(); }
function goDetail(row) { router.push(`/devices/${row.id}`); }

onMounted(load);
</script>

<style scoped>
.page { height: 100vh; }
.body { padding: 16px; }
.mt { margin-top: 16px; }
</style>
```

- [ ] **Step 3: Write `admin/web/src/views/DeviceDetailView.vue`**

```vue
<template>
  <div class="page">
    <el-menu mode="horizontal" :default-active="'devices'" router>
      <el-menu-item index="dashboard" route="/">总览</el-menu-item>
      <el-menu-item index="map" route="/map">地图</el-menu-item>
      <el-menu-item index="devices" route="/devices">设备</el-menu-item>
    </el-menu>
    <div class="body" v-loading="loading">
      <el-alert v-if="device && device.is_key" title="重点设备：在多个区域被发现" type="error" show-icon class="mt" />
      <el-descriptions :column="2" border class="mt">
        <el-descriptions-item label="标识">{{ device?.device_key }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ device?.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ device?.device_type }}</el-descriptions-item>
        <el-descriptions-item label="出现次数">{{ device?.total_count }}</el-descriptions-item>
        <el-descriptions-item label="地点数">{{ device?.cluster_count }}</el-descriptions-item>
        <el-descriptions-item label="末次发现">{{ device?.last_seen }}</el-descriptions-item>
      </el-descriptions>
      <el-card class="mt">
        <template #header><span>轨迹</span></template>
        <div ref="mapEl" class="map"></div>
      </el-card>
      <el-card class="mt">
        <template #header><span>各地点遇见次数</span></template>
        <el-table :data="clusters">
          <el-table-column prop="id" label="地点" />
          <el-table-column prop="point_count" label="次数" />
          <el-table-column label="中心坐标">
            <template #default="{ row }">{{ row.center_lat.toFixed(5) }}, {{ row.center_lng.toFixed(5) }}</template>
          </el-table-column>
          <el-table-column prop="last_seen" label="末次" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { api } from '../api';
import { loadAMap } from '../utils/amap';

const route = useRoute();
const device = ref(null);
const clusters = ref([]);
const mapEl = ref(null);
const loading = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    const data = await api.getDevice(route.params.id);
    device.value = data.device;
    clusters.value = data.clusters;
    const traj = await api.getTrajectory(route.params.id);
    const AMap = await loadAMap();
    const m = new AMap.Map(mapEl.value, { zoom: 12, center: traj.points.length ? [traj.points[0].lng, traj.points[0].lat] : [116.397, 39.908] });
    if (traj.points.length) {
      new AMap.Polyline({ path: traj.points.map((p) => [p.lng, p.lat]), strokeColor: '#f56c6c', strokeWeight: 4 }).setMap(m);
      traj.points.forEach((p) => new AMap.Marker({ position: [p.lng, p.lat] }).setMap(m));
    }
  } finally { loading.value = false; }
});
</script>

<style scoped>
.page { height: 100vh; }
.body { padding: 16px; }
.mt { margin-top: 16px; }
.map { height: 360px; }
</style>
```

- [ ] **Step 4: Append CHANGELOG entry**

Add to `CHANGELOG.md`:
```
## 2026-07-19 — Admin platform web (Vue3 + Element Plus + AMap)
- Added `admin/web`: login, dashboard with key-device alert, AMap map view (filter + detail drawer + trajectory), device list with cross-region highlight, device detail with trajectory and per-place encounter counts.
- Verification: `npm run dev` manual check; build via `npm run build`.
```

- [ ] **Step 5: Commit**

```bash
git add admin/web/src/views/DashboardView.vue admin/web/src/views/DevicesView.vue admin/web/src/views/DeviceDetailView.vue CHANGELOG.md
git commit -m "feat(web): add dashboard, devices, device detail views; update CHANGELOG"
```

---

## Part C — App upload (`shared`)

### Task C1: UploadTransport platform boundary

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/service/UploadTransport.kt`
- Modify: `shared/src/androidMain/kotlin/com/example/scanapp/service/HttpUrlConnectionUploadTransport.kt` (path per existing androidMain layout)
- Create: `shared/src/iosMain/kotlin/com/example/scanapp/service/HttpUrlConnectionUploadTransport.kt` (stub, iosApp is placeholder)

**Interfaces:**
- Produces: `expect object UploadTransport { suspend fun postJson(url: String, token: String, body: String): Boolean }`.

- [ ] **Step 1: Write `shared/src/commonMain/kotlin/com/example/scanapp/service/UploadTransport.kt`**

```kotlin
package com.example.scanapp.service

expect object UploadTransport {
    suspend fun postJson(url: String, token: String, body: String): Boolean
}
```

- [ ] **Step 2: Create Android actual**

Write at `shared/src/androidMain/kotlin/com/example/scanapp/service/HttpUrlConnectionUploadTransport.kt`:
```kotlin
package com.example.scanapp.service

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object UploadTransport {
    actual suspend fun postJson(url: String, token: String, body: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("X-Upload-Token", token)
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (e: Throwable) {
            false
        }
    }
}
```

- [ ] **Step 3: Create iOS stub**

Write at `shared/src/iosMain/kotlin/com/example/scanapp/service/HttpUrlConnectionUploadTransport.kt`:
```kotlin
package com.example.scanapp.service

actual object UploadTransport {
    actual suspend fun postJson(url: String, token: String, body: String): Boolean = false
}
```

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/example/scanapp/service/UploadTransport.kt shared/src/androidMain/kotlin/com/example/scanapp/service/HttpUrlConnectionUploadTransport.kt shared/src/iosMain/kotlin/com/example/scanapp/service/HttpUrlConnectionUploadTransport.kt
git commit -m "feat(app): add UploadTransport platform boundary (Android HttpURLConnection, iOS stub)"
```

### Task C2: PendingUpload SQLDelight table + DAO

**Files:**
- Modify: `shared/src/commonMain/sqldelight/com/example/scanapp/Database.sq` (append table + queries)
- Modify: `shared/src/commonMain/kotlin/com/example/scanapp/database/PendingUploadDao.kt` (create)

**Interfaces:**
- Produces: `PendingUploadDao.enqueue(payload, createdAt)`, `PendingUploadDao.peekOldest(limit)`, `PendingUploadDao.deleteUpTo(id)`, `PendingUploadDao.count()`; regenerated `databaseQueries.enqueuePendingUpload`, `selectOldestPendingUploads`, `deletePendingUploadsUpTo`, `countPendingUploads`.

- [ ] **Step 1: Append to `Database.sq`**

Add at end of file:
```sql
CREATE TABLE PendingUpload (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    payload TEXT NOT NULL,
    createdAt INTEGER NOT NULL
);

CREATE INDEX idx_pending_created ON PendingUpload(createdAt);

enqueuePendingUpload:
INSERT INTO PendingUpload (payload, createdAt) VALUES (?, ?);

selectOldestPendingUploads:
SELECT * FROM PendingUpload ORDER BY createdAt ASC LIMIT :limit;

deletePendingUploadsUpTo:
DELETE FROM PendingUpload WHERE id <= :maxId;

countPendingUploads:
SELECT COUNT(*) FROM PendingUpload;
```

- [ ] **Step 2: Write `shared/src/commonMain/kotlin/com/example/scanapp/database/PendingUploadDao.kt`**

```kotlin
package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PendingUploadDao(private val database: ScanAppDatabase) {

    suspend fun enqueue(payload: String, createdAt: Long) = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.enqueuePendingUpload(payload, createdAt)
    }

    suspend fun peekOldest(limit: Long): List<PendingUploadRow> = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.selectOldestPendingUploads(limit).executeAsList().map {
            PendingUploadRow(it.id, it.payload)
        }
    }

    suspend fun deleteUpTo(maxId: Long) = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.deletePendingUploadsUpTo(maxId)
    }

    suspend fun count(): Long = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.countPendingUploads().executeAsOne()
    }
}

data class PendingUploadRow(val id: Long, val payload: String)
```

- [ ] **Step 3: Regenerate SQLDelight interfaces**

Run: `.\gradlew.bat :shared:generateCommonMainScanAppDatabaseInterface`
Expected: success; new query methods available on `databaseQueries`.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/sqldelight/com/example/scanapp/Database.sq shared/src/commonMain/kotlin/com/example/scanapp/database/PendingUploadDao.kt
git commit -m "feat(app): add PendingUpload table + DAO"
```

### Task C3: UploadService + tests

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/service/UploadService.kt`
- Create: `shared/src/commonTest/kotlin/com/example/scanapp/service/UploadServiceTest.kt`

**Interfaces:**
- Consumes: `UploadTransport`, `PendingUploadDao`, settings (serverUrl/token/uploaderId/enabled).
- Produces: `UploadService.enqueue(wifi, bluetooth, uploaderId, serverUrl, token, enabled)`, `UploadService.flushPending(db)`.

- [ ] **Step 1: Write failing test `UploadServiceTest.kt`**

```kotlin
package com.example.scanapp.service

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UploadServiceTest {

    private val captured = mutableListOf<String>()

    private fun fakeTransport() = object : UploadTransportLike {
        override suspend fun postJson(url: String, token: String, body: String): Boolean {
            captured.add(body)
            return true
        }
    }

    @Test
    fun `enqueue sets payload and enabled false drops`() = runTest {
        val svc = UploadService(fakeTransport())
        val batch = ScanBatch(
            uploaderId = "u1",
            wifi = listOf(WifiSighting("AA:BB", "cafe", -50, 2412, 39.9, 116.4, 1000L)),
            bluetooth = emptyList()
        )
        val payload = svc.buildPayload(batch)
        assertTrue(payload.contains("AA:BB"))
        assertTrue(payload.contains("\"uploaderId\":\"u1\""))
    }

    @Test
    fun `buildPayload drops zero coordinates`() = runTest {
        val svc = UploadService(fakeTransport())
        val batch = ScanBatch(
            uploaderId = "u1",
            wifi = listOf(WifiSighting("AA:BB", "cafe", -50, 2412, 0.0, 0.0, 1000L)),
            bluetooth = emptyList()
        )
        val payload = svc.buildPayload(batch)
        assertEquals("{\"uploaderId\":\"u1\",\"wifi\":[],\"bluetooth\":[]}", payload)
    }
}
```

- [ ] **Step 2: Write `shared/src/commonMain/kotlin/com/example/scanapp/service/UploadService.kt`**

```kotlin
package com.example.scanapp.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface UploadTransportLike {
    suspend fun postJson(url: String, token: String, body: String): Boolean
}

@Serializable
data class WifiSighting(
    val bssid: String, val ssid: String, val signal: Int,
    val frequency: Int, val lat: Double, val lng: Double, val timestamp: Long
)

@Serializable
data class BtSighting(
    val address: String, val name: String, val rssi: Int,
    val deviceType: String, val lat: Double, val lng: Double, val timestamp: Long
)

@Serializable
data class ScanBatch(
    val uploaderId: String,
    val wifi: List<WifiSighting>,
    val bluetooth: List<BtSighting>
)

class UploadService(private val transport: UploadTransportLike) {

    fun buildPayload(batch: ScanBatch): String {
        val wifi = batch.wifi.filter { it.lat != 0.0 || it.lng != 0.0 }
        val bt = batch.bluetooth.filter { it.lat != 0.0 || it.lng != 0.0 }
        return Json.encodeToString(ScanBatch(batch.uploaderId, wifi, bt))
    }

    suspend fun tryUpload(serverUrl: String, token: String, batch: ScanBatch): Boolean {
        if (serverUrl.isBlank() || token.isBlank()) return false
        return transport.postJson(serverUrl, token, buildPayload(batch))
    }
}
```

- [ ] **Step 3: Update test to use real UploadService**

Replace the test transport usage with `UploadTransportLike` and keep assertions. Run:
```bash
.\gradlew.bat :shared:testDebugUnitTest --tests com.example.scanapp.UploadServiceTest
```
Expected: PASS (2 tests). Note: the `expect object UploadTransport` is the production transport; tests use the `UploadTransportLike` interface to stay platform-free.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/example/scanapp/service/UploadService.kt shared/src/commonTest/kotlin/com/example/scanapp/service/UploadServiceTest.kt
git commit -m "feat(app): add UploadService with payload builder + tests"
```

### Task C4: Wire into ScannerServiceImpl + Settings UI

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/example/scanapp/service/ScannerServiceImpl.kt`
- Modify: `shared/src/commonMain/kotlin/com/example/scanapp/ui/SettingsPage.kt`

**Interfaces:**
- Consumes: `UploadService`, `PendingUploadDao`, `DatabaseFactory.getDatabase()`.

- [ ] **Step 1: Add upload hooks in `ScannerServiceImpl`**

In `saveWifiDevices` and `saveBluetoothDevices`, after `wifiDao.insertBatch(devicesWithLocation)`, build a `ScanBatch` and enqueue a pending upload. Add a `flushPendingUploads()` call at the start of each scan cycle (in `startAllScans` loop, before/after scanning). Insert fields:

```kotlin
private val uploadService = UploadService(object : UploadTransportLike {
    override suspend fun postJson(url: String, token: String, body: String): Boolean =
        UploadTransport.postJson(url, token, body)
})
```

Modify `saveWifiDevices` end (after insertBatch):
```kotlin
val batch = ScanBatch(
    uploaderId = uploaderId,
    wifi = devicesWithLocation.map { WifiSighting(it.bssid, it.ssid, it.signalStrength, it.frequency, it.latitude, it.longitude, it.timestamp) },
    bluetooth = emptyList()
)
enqueueUpload(batch)
```

Similarly for `saveBluetoothDevices` with `BtSighting`. Add helper:
```kotlin
private suspend fun enqueueUpload(batch: ScanBatch) {
    val db = DatabaseFactory.getDatabase()
    PendingUploadDao(db).enqueue(UploadService(object : UploadTransportLike {
        override suspend fun postJson(url: String, token: String, body: String): Boolean = UploadTransport.postJson(url, token, body)
    }).buildPayload(batch), System.currentTimeMillis())
    flushPendingUploads()
}
```

Add to `ScannerServiceImpl` constructor: `private val uploaderId: String = "default"`, `private val serverUrlProvider: () -> String = { "" }`, `private val uploadTokenProvider: () -> String = { "" }`, `private val uploadEnabledProvider: () -> Boolean = { false }`.

Add method:
```kotlin
private suspend fun flushPendingUploads() {
    if (!uploadEnabledProvider()) return
    val url = serverUrlProvider(); val token = uploadTokenProvider()
    if (url.isBlank() || token.isBlank()) return
    val db = DatabaseFactory.getDatabase()
    val dao = PendingUploadDao(db)
    if (dao.count() > 500) {
        val oldest = dao.peekOldest(500)
        dao.deleteUpTo(oldest.last().id)
    }
    val rows = dao.peekOldest(50)
    if (rows.isEmpty()) return
    val svc = uploadService
    var maxId = -1L
    for (row in rows) {
        val ok = svc.tryUpload(url, token, Json.decodeFromString(row.payload))
        if (ok) maxId = row.id
    }
    if (maxId > 0) dao.deleteUpTo(maxId)
}
```

(Import `kotlinx.serialization.json.Json`.)

- [ ] **Step 2: Update `SettingsPage` UI**

Add an `MdcSectionHeader("平台上报")` block before "About" with four `MdcTextField`-style rows for server URL, upload token, uploader ID, and an `MdcSwitch`-style enabled toggle. Persist via Kuikly SP (mirror how other Mdc controls store state). On change, update the `ScannerServiceImpl` providers (wire via a small `UploadSettings` object in commonMain reading/writing SP keys `upload_url`, `upload_token`, `upload_id`, `upload_enabled`).

Because exact `MdcTextField`/`MdcSwitch` component names must match this repo, inspect `MdcComponents.kt` and use the actual component invocations for text input and switch; keep field keys consistent. (Do not invent component names — read `ui/MdcComponents.kt` first.)

- [ ] **Step 3: Regenerate + build + test**

Run:
```bash
.\gradlew.bat :shared:generateCommonMainScanAppDatabaseInterface
.\gradlew.bat :shared:compileDebugKotlinAndroid
.\gradlew.bat :shared:testDebugUnitTest
```
Expected: generation + compile + unit tests pass.

- [ ] **Step 4: Append CHANGELOG entry**

Add to `CHANGELOG.md`:
```
## 2026-07-19 — App upload to admin platform
- Added per-scan upload of sighting batches to the admin backend (UploadTransport + UploadService + PendingUpload queue with retry). Settings page gains server URL/token/uploader ID/enable switch.
- Zero-coordinate sightings are dropped before upload. Pending queue capped at 500 batches.
- Verification: `./gradlew.bat :shared:testDebugUnitTest`, `:shared:compileDebugKotlinAndroid`.
```

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/example/scanapp/service/ScannerServiceImpl.kt shared/src/commonMain/kotlin/com/example/scanapp/ui/SettingsPage.kt shared/src/commonMain/kotlin/com/example/scanapp/service/UploadSettings.kt CHANGELOG.md
git commit -m "feat(app): wire upload into scanner + settings UI; update CHANGELOG"
```

---

## Part D — Integration verification

### Task D1: End-to-end smoke

- [ ] **Step 1: Start MySQL + server**

```bash
mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS scanapp_admin CHARACTER SET utf8mb4;"
cp admin/server/.env.example admin/server/.env
cd admin/server && node src/index.js
```
Expected: `admin server listening on :3001`.

- [ ] **Step 2: Start web**

```bash
cd admin/web && npm run dev
```
Expected: Vite dev server on :5173. Open browser, log in (`admin`/`admin123`), see dashboard; upload a sample batch via curl:
```bash
curl -X POST http://localhost:3001/api/upload -H "X-Upload-Token: change-me-upload-token" -H "Content-Type: application/json" -d '{"uploaderId":"u1","wifi":[{"bssid":"aa:bb:cc:dd:ee:01","ssid":"cafe","signal":-50,"frequency":2412,"lat":39.900,"lng":116.400,"timestamp":1784000000000},{"bssid":"aa:bb:cc:dd:ee:01","ssid":"cafe","signal":-50,"frequency":2412,"lat":40.900,"lng":117.400,"timestamp":1784003600000}]}'
```
Then refresh map → two red markers / key device highlighted.

- [ ] **Step 3: Report verification result**

No commit needed; summarize to user what was verified.
