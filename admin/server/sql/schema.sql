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
  `signal` INT NOT NULL,
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
