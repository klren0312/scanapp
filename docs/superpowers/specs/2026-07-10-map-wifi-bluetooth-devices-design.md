# 设计文档：地图显示 WiFi 与蓝牙设备

- 日期：2026-07-10
- 状态：已确认，待实现

## 背景

应用导航栏的 "Map" 页面由 `androidApp/.../KRRouterAdapter.kt:11` 路由到原生
`OsmMapActivity`（基于 OSMDroid 的真实地图）。该 Activity 当前只标绘 **Location（GPS 采样）记录**，
用户期望在地图上看到**扫描到的设备**。

本次将 `OsmMapActivity` 改为标绘 **WiFi 与蓝牙设备记录（带经纬度）**，点击图钉弹出信息卡。

约束（来自需求澄清）：
- 平台：仅 Android 优先（iOS/鸿蒙暂不涉及）。
- 地图 SDK：OSMDroid（OpenStreetMap，免费、无需 API Key）。
- 标注内容：WiFi + 蓝牙设备，按类型区分颜色。
- 交互：点击图钉显示信息卡（OSMDroid 默认信息气泡），不跳转。

## 范围

仅修改 `androidApp/src/main/java/com/example/scanapp/OsmMapActivity.kt`。
不改动 Kuikly `MapPage`（路由已绕过，属死代码，本次不清理）。

## 现有实现要点（OsmMapActivity）

- 已集成 OSMDroid：`MapView` + `MAPNIK` 瓦片源，支持多点触控。
- `loadLocations()` 在 `onCreate` 读取 `LocationDao.getAllRecords()`。
- `renderLocations()` 过滤有效坐标后逐个添加 `Marker`，无效时缩放到 (0,0) zoom 3。
- 已有抽屉导航、`mapView` 生命周期（onResume/onPause/onDestroy）、`dp()`/`formatOneDecimal()` 辅助方法。

## 改动设计

1. **数据来源**
   - 替换 `loadLocations()` 为 `loadDevices()`：
     - `WifiScanDao(db).getAllRecords()` → WiFi 设备。
     - `BluetoothScanDao(db).getAllRecords()` → 蓝牙设备。
   - 数据库通过 `DatabaseFactory.getDatabase()` 获取（与现有 DAO 用法一致）。

2. **坐标有效性过滤**
   - 跳过 `latitude == 0.0 && longitude == 0.0`（无 GPS 时的默认值）。
   - 跳过超出 `[-90,90]` / `[-180,180]` 的点。
   - 合并两类有效记录后标绘。

3. **按类型区分颜色**
   - 新增辅助方法 `makeMarkerIcon(color: Int): Drawable`，生成一个彩色圆形 `BitmapDrawable`。
   - WiFi 用蓝色（如 `0xFF1565C0`），蓝牙用紫色（如 `0xFF6A1B9A`）。
   - 通过 `Marker.setIcon(drawable)` 设置。

4. **标记内容**
   - WiFi：`title = ssid`（为空显示 "Unknown"），`snippet = "${signalStrength} dBm\n${bssid}"`。
   - 蓝牙：`title = name`（为空显示 "Unknown"），`snippet = "${rssi} dBm\n${address}"`。
   - `setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)`。
   - 点击图钉由 OSMDroid 默认信息气泡显示 title/snippet（即"信息卡"），无需自定义覆盖层。

5. **视图与缩放**
   - 存在有效点时：缩放至合适级别（如 zoom 16 或计算边界 `BoundingBox` 后 `zoomToBoundingBox`），居中到中心点。
   - 无有效点时：`statusView` 提示 "No device records with location"，地图保持默认视图。

6. **状态栏**
   - `statusView` 显示两类数量，例如：`"WiFi: ${wifiCount}  Bluetooth: ${btCount}"`。

7. **实时刷新**
   - 进入后启动定时轮询：每 3 秒调用 `loadDevices()` 重新查询并重建标记（与 App 其它页面节奏一致）。
   - 轮询在 `onPause` 暂停、`onResume` 恢复、`onDestroy` 取消（`CoroutineScope` 已在 `onDestroy` 取消，需确保轮询随生命周期启停）。
   - 重建标记前先 `mapView.overlays.clear()`，再添加新标记并 `invalidate()`。

8. **保持不变**
   - 抽屉导航（navigateFromDrawer）、`mapView` 生命周期转发、权限无关逻辑。

## 数据流

`DatabaseFactory.getDatabase()` → `WifiScanDao` / `BluetoothScanDao` → 有效坐标过滤
→ 生成彩色 `Marker` → `mapView.overlays` → 用户点击 → OSMDroid 信息气泡。

## 错误处理

- 查询/标绘失败：`statusView` 显示 `"Failed to load devices: ${message}"`（沿用现有 `.onFailure` 模式）。
- 全部坐标无效：状态栏提示无位置数据，地图不崩溃。

## 验证

- `./gradlew :androidApp:assembleDebug` 编译通过。
- 手动：进入 Map 页面，确认 WiFi/蓝牙设备以不同颜色标绘；点击图钉显示 SSID/BSSID 或名称/地址信息卡；
  扫描进行中时标记随时间更新；无有效坐标时不崩溃并给出提示。
- 若无法在本地运行模拟器，在 CHANGELOG 中记录未做运行时验证。
