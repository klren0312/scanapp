## 2026-07-19 — App upload to admin platform
- Added per-scan upload of sighting batches to the admin backend (UploadTransport + UploadService + PendingUpload queue with retry). Settings page gains server URL/token/uploader ID/enable switch.
- Zero-coordinate sightings are dropped before upload. Pending queue capped at 500 batches.
- Verification: `./gradlew.bat :shared:testDebugUnitTest`, `:shared:compileDebugKotlinAndroid`.

## 2026-07-19 — Admin platform web (Vue3 + Element Plus + AMap)
- Added `admin/web`: login, dashboard with key-device alert, AMap map view (filter + detail drawer + trajectory), device list with cross-region highlight, device detail with trajectory and per-place encounter counts.
- Verification: `npm run dev` manual check; build via `npm run build`.

## 2026-07-19 — Admin web map view
- Added `admin/web/src/utils/amap.js` (AMap JS loader) and `admin/web/src/views/MapView.vue`: GCJ-02 map markers (key devices red), filter by keyword/type/keyOnly, click opens detail drawer with trajectory polyline + per-place encounter counts.
- Verification: manual (needs AMap key + running server).

## 2026-07-19 — Admin web login view
- Added `admin/web/src/views/LoginView.vue`: admin username/password login, JWT stored via auth store, redirects to dashboard.

## 2026-07-19 — Admin web scaffold (Vue3 + Element Plus)
- Added `admin/web`: Vite + Vue3 app shell, pinia auth store, axios API client (JWT interceptor), vue-router with auth guard, AMap env config.
- Verification: `npm install` clean; full build deferred until views exist (B4).

## 2026-07-19 — Admin platform backend (Node.js + MySQL)
- Added `admin/server`: Express API for device upload, distance-clustering, JWT admin login, map/stats/device queries, cluster recompute.
- Key-device rule: device seen in >=2 clusters (500m radius) flagged `is_key`.
- Verification: `node --test test/` (geo, cluster, server smoke). Local DB root/root, db `scanapp_admin`.

