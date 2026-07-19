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
