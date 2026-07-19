const test = require('node:test');
const assert = require('node:assert');
const request = require('supertest');
const express = require('express');
const cors = require('cors');
const { config } = require('../src/config');
const { getPool, initSchema } = require('../src/db');
const { runSeed } = require('../src/seed');
const { router: authRouter } = require('../src/auth');
const { router: uploadRouter } = require('../src/routes/upload');
const { router: devicesRouter } = require('../src/routes/devices');
const { router: statsRouter } = require('../src/routes/stats');

test.before(async () => {
  await initSchema();
  await runSeed();
});

test.after(async () => {
  const pool = getPool();
  if (pool && pool.end) await pool.end();
});

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
