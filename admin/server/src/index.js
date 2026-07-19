const express = require('express');
const cors = require('cors');
const { initSchema } = require('./db');
const { runSeed } = require('./seed');
const { config } = require('./config');
const { router: authRouter } = require('./auth');
const { router: uploadRouter } = require('./routes/upload');
const { router: devicesRouter } = require('./routes/devices');
const { router: mapRouter } = require('./routes/map');
const { router: statsRouter } = require('./routes/stats');
const { router: adminRouter } = require('./routes/admin');

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
