## 2026-07-19 — Admin web login view
- Added `admin/web/src/views/LoginView.vue`: admin username/password login, JWT stored via auth store, redirects to dashboard.

## 2026-07-19 — Admin web scaffold (Vue3 + Element Plus)
- Added `admin/web`: Vite + Vue3 app shell, pinia auth store, axios API client (JWT interceptor), vue-router with auth guard, AMap env config.
- Verification: `npm install` clean; full build deferred until views exist (B4).

## 2026-07-19 — Admin platform backend (Node.js + MySQL)
- Added `admin/server`: Express API for device upload, distance-clustering, JWT admin login, map/stats/device queries, cluster recompute.
- Key-device rule: device seen in >=2 clusters (500m radius) flagged `is_key`.
- Verification: `node --test test/` (geo, cluster, server smoke). Local DB root/root, db `scanapp_admin`.

