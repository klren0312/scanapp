# Changelog

## 2026-07-13

- Removed the native Android title bar (ActionBar showing "ScanApp") by switching the application theme from `Theme.AppCompat.Light.DarkActionBar` to `Theme.AppCompat.Light.NoActionBar` in `AndroidManifest.xml`. Kuikly UI top bars are unchanged.
- Audited every page's refresh logic for the same post-destroy mutation crash seen in `DeviceListPage`. Applied the same fix (introduce `isPageActive`, reset it in `pageWillDestroy`, guard the refresh loop and `refreshData`, and drop the broken `observableList != List` identity check) to `StatisticsPage`, `MapPage`, and `ScannerPage` (Scanner's scan loop now also stops on destroy).
- Reworked `DeviceListPage` per request: switched from auto-refresh every 3s to **manual refresh** (a `Refresh` button; initial load on `created`). Added a **device-type filter** (All / WiFi / Bluetooth) via `MdcTab`. Collapsed the old two-column WiFi|Bluetooth layout into a **single-column list** backed by a unified `DeviceItem` `observableList`.
- Switched `StatisticsPage` and `MapPage` from the 3s auto-polling loop to **manual refresh** (a `Refresh` button; initial load on `created`). The `isPageActive` destroy guard and `refreshData` suspend refactor from the earlier audit are retained.
- Made `MdcTab`'s `selected` parameter reactive (`() -> Boolean`) so the filter highlight updates on selection. `MdcTab` was previously unused, so this is safe.
- Verification: not compiled (per request); changes reuse existing `DatabaseFactory`/`Dao`/`observableList`/`MdcTab`/`MdcDeviceCard` APIs and mirror the `isPageActive` pattern from `DeviceDetailPage`.

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
