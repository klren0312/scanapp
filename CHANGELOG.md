# Changelog

## 2026-07-10

- Real-time refresh for list pages: `DeviceListPage`, `StatisticsPage`, and `MapPage` now reload data every 3s while visible. Device/location lists were changed from plain `var` to `observable` so asynchronously loaded data renders (previously the lists never displayed after the initial async load).
- Map screen (`OsmMapActivity`) now plots scanned devices instead of raw GPS samples: WiFi (blue markers) and Bluetooth (purple markers) with color-coded icons. Tap a marker to show an info bubble (SSID/BSSID or name/address + signal). Invalid coordinates (0,0 / out of range) are filtered out, bounds auto-fit, and markers refresh every 3s while the page is visible.
- Design doc: `docs/superpowers/specs/2026-07-10-map-wifi-bluetooth-devices-design.md`.
- Verification: not compiled (per request); relies on standard OSMDroid APIs (`BoundingBox.fromGeoPoints`, `MapView.zoomToBoundingBox`, `Marker.setIcon`).

## 2026-07-07

- Added `agent.md` with project-specific agent workflow, architecture notes, verification commands, changelog requirements, and commit rules.
- Verification: documentation-only change; no runtime verification required.
