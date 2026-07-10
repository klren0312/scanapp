# Changelog

## 2026-07-10

- Fixed Kuikly reactive rendering across scanner and data pages: values are now read inside `attr {}` through suppliers, dynamic collections use `observableList` with `vfor`/`vforIndex`, and empty/export states use `vif`. Scanner results now refresh once per second while scanning, device/statistics/location lists render without page switching, and device-detail fields update after asynchronous loading.
- Failed scan startup now resets the scanning state instead of leaving the UI stuck on `Stop Scanning`.
- Verification: `./gradlew.bat :shared:compileDebugKotlinAndroid` and `./gradlew.bat :androidApp:assembleDebug` (passed). `./gradlew.bat :shared:testDebugUnitTest` remains blocked by pre-existing test-source compilation errors (`JdbcSqliteDriver` missing and outdated model constructor calls).
- Map screen (`OsmMapActivity`) now plots scanned devices instead of raw GPS samples: WiFi (blue markers) and Bluetooth (purple markers) with color-coded icons. Tap a marker to show an info bubble (SSID/BSSID or name/address + signal). Invalid coordinates (0,0 / out of range) are filtered out, bounds auto-fit, and markers refresh every 3s while the page is visible.
- Design doc: `docs/superpowers/specs/2026-07-10-map-wifi-bluetooth-devices-design.md`.
- Verification: not compiled (per request); relies on standard OSMDroid APIs (`BoundingBox.fromGeoPoints`, `MapView.zoomToBoundingBox`, `Marker.setIcon`).

## 2026-07-07

- Added `agent.md` with project-specific agent workflow, architecture notes, verification commands, changelog requirements, and commit rules.
- Verification: documentation-only change; no runtime verification required.
