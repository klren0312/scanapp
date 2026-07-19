## 2026-07-19 — Admin platform backend (Node.js + MySQL)
- Added `admin/server`: Express API for device upload, distance-clustering, JWT admin login, map/stats/device queries, cluster recompute.
- Key-device rule: device seen in >=2 clusters (500m radius) flagged `is_key`.
- Verification: `node --test test/` (geo, cluster, server smoke). Local DB root/root, db `scanapp_admin`.

