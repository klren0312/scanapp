# Changelog


- Fix compile error in DeviceListPage: the pre-existing MdcBodyText Loading more string literal was corrupted (mojibake) into an unterminated string, breaking the build. Restored to 'Loading more...'.


- Cell (base station) permission prompt is now actionable: the missing-permission hint on both the Scanner page and Device List (Cell tab) gained a **Grant location permission** button. Tapping it re-requests the runtime location permission via a new requestCellScanPermission() (Android shows the OS dialog; iOS/ohos no-op). Android bridges this through ActivityHolder (current KuiklyRenderActivity tracked on resume/clear on pause/destroy) and the existing PermissionHelper.

- Fixed a stray brace in ScannerPage.computeCellHint left over from the prior refactor so the page compiles cleanly.


- Reused the Cell-readiness hint on both pages: cellReadinessHint(count) now lives in commonMain (PlatformScanController) and returns an empty string when the count is positive. The Scanner status line shows the hint while scanning (instead of the duplicate dedicated block), and the Device List now shows the same hint in the empty state when the **Cell** tab is selected and no cells were found.


- Scanner page now explains *why* the Cell (base station) count is zero: added a shared getCellScanReadiness() diagnostic (READY / MISSING_PERMISSION / UNSUPPORTED) with Android checking location permission and iOS/ohos returning UNSUPPORTED. When Cell count is 0 the Scanner shows a hint (grant location permission, not supported on this platform, or move outdoors/wait a few cycles).


- Fix cell (base station) scans returning nothing on Android 10+: scanCellInfo() previously fire-and-forgot requestCellInfoUpdate and then read the synchronous allCellInfo immediately, which is still empty until the async callback fires, so each cycle stored zero cells. It now awaits the fresh snapshot from requestCellInfoUpdate (<=3s) and only falls back to the cached allCellInfo on timeout/error. Called from background coroutine dispatchers (IO/Default), so the bounded wait is safe.

## 2026-07-16

- Scanner page is now **count-only**: removed the recent-scan card list and the per-second merged-list rebuild that made the page janky. While scanning it only refreshes WiFi/Bluetooth/Cell totals every 3s, with a shortcut to Device List for full records.
- Cell (base station) scanning hardened on Android:
  - Runtime request for `READ_PHONE_STATE` (already in the manifest) so cell reads are not silently empty.
  - Dropped the incorrect API 30-only annotation; cell scan now works from minSdk 26 with API-safe identity parsing.
  - Added `requestCellInfoUpdate` refresh, TD-SCDMA support, NR TAC/NCI with ARFCN/PCI fallback, CDMA no longer filtered out for missing MCC/MNC, and better permission/exception logging.
- Device List remains the only place that shows full scan result lists.
- Verification: not compiled (per `agent.md`); scoped UI/permission/scanner changes only.

## 2026-07-15

- Added **Cell (base station) scanning** for Android, available alongside WiFi and Bluetooth. Cells are polled via `TelephonyManager.getAllCellInfo()` on the same intervals as the background/worker scans (LTE/WCDMA/GSM/NR/CDMA; CDMA maps networkId鈫扡AC, systemId鈫扖ID). Only Android implements real cell data; iOS/ohos are unaffected and still compile.
- New `CellScanRecord` SQLDelight table + `CellScanDao`, surfaced through a unified `cellKey` (networkType:mcc:mnc:lac:cid). Insert-or-update preserves previously stored valid coordinates when a newer scan lacks a GPS fix (same rule as WiFi/Bluetooth).
- UI: Scanner page shows a **Cell** count badge and merges cells into the recent-scans list; Device List gains a **Cell** tab with pagination and deep-link to detail; Device Detail renders a Cell branch (network/operator/MCC/MNC/LAC/CID/signal) with coordinate fallback to the nearest stored location; Statistics adds a **Cell** total + **Top Cell** ranking.
- `AndroidManifest.xml`: added `READ_PHONE_STATE` for cell info.
- Verification: not compiled (per `agent.md`); changes mirror the existing WiFi/Bluetooth `Dao`/`observableList`/`MdcTab`/`MdcRankingRow` patterns and reuse `DatabaseFactory.dbDispatcher` for all DB access.

- Fixed stale data on pages that were opened before a scan ran: `StatisticsPage`, `ScannerPage`, `DeviceListPage`, and `SettingsPage` now reload their data in `pageDidAppear()` (Kuikly `Pager` lifecycle), so returning to a page after a background/worker scan refreshes the counts and lists instead of showing the old values from `created()`.

- Fixed runtime `no such table: CellScanRecord` on already-installed databases. SQLDelight only created the new table in fresh DBs; existing DBs stayed at schema version 1. Added SQLDelight migrations `migrations/1.sqm` (original Wifi/Bluetooth/Location schema) and `migrations/2.sqm` (adds `CellScanRecord` + its indexes) so installed databases are upgraded on launch instead of crashing on the cell count query.
- Corrected the migrations to be idempotent (`IF NOT EXISTS` on every `CREATE TABLE`/`CREATE INDEX`). SQLDelight computes `Schema.version = numMigrations + 1` (=3), so upgrading a v1 DB runs **both** `1.sqm` and `2.sqm`; without `IF NOT EXISTS` it re-ran the base-table `CREATE`s and threw `table WifiScanRecord already exists`. With `IF NOT EXISTS` the base tables are a no-op and only `CellScanRecord` is added.

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
