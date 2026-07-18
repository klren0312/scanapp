# 基站扫描功能设计（Cell / Base Station Scanning）

日期：2026-07-15

## 目标

为 scanapp 增加"基站扫描"能力：在 Android 上通过 `TelephonyManager.getAllCellInfo()`
定时轮询当前可见小区，作为第三种设备类型（`cell`）与现有 WiFi/蓝牙统一存储、展示与查看。
iOS/ohos 本次不实现真实数据（仅保持编译通过，无实际扫描）。

## 范围与约束

- 平台：仅 Android 有真实基站数据；commonMain 定义模型/DAO/接口，androidMain 提供实现。
- 展示形态：并入现有设备列表（ScannerPage / DeviceListPage / DeviceDetailPage / StatisticsPage），
  不作为独立页面。
- 采集字段：核心字段（网络类型、MCC、MNC、LAC/TAC、CID/CI、信号强度 dBm、运营商名、
  timestamp、经纬度、出现次数）。
- 扫描方式：定时轮询 `getAllCellInfo()`，与现有 WiFi 轮询频率一致。
- 权限：Android 10+ 必须授予定位权限 `getAllCellInfo()` 才有数据；需 `READ_PHONE_STATE`。

## 架构

完全复用现有 WiFi/蓝牙的分层结构：

```
Database.sq (CellScanRecord 表 + 查询)
models/CellScanRecord.kt
database/CellScanDao.kt            (复用 DatabaseFactory.dbDispatcher 单线程调度)
platform/AndroidCellScanner.kt     (androidMain, 封装 TelephonyManager)
service/BackgroundScanService.kt   (主后台扫描路径，加入基站轮询)
service/AndroidScannerService.kt   (WorkManager worker，加入基站轮询)
ui/ScannerPage.kt                  (合并基站到设备流)
ui/DeviceListPage.kt               (Cell 过滤 tab + 加载/分页/跳转)
ui/DeviceDetailPage.kt             (cell 详情分支 + 地图)
ui/StatisticsPage.kt               (Cell 统计 + Top Cell 排行)
androidApp/.../AndroidManifest.xml (权限补充)
```

`ScannerService` 接口与 `ScannerServiceImpl`（commonMain）**不改动**：
实际前台扫描走 `PlatformScanController.startBackgroundScanning()` → `BackgroundScanService`，
UI 通过读数据库获取基站记录，与 WiFi/蓝牙一致。

## 数据模型

### Database.sq

新增表：

```sql
CREATE TABLE CellScanRecord (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    networkType TEXT NOT NULL,
    mcc INTEGER NOT NULL,
    mnc INTEGER NOT NULL,
    lac INTEGER NOT NULL,
    cid INTEGER NOT NULL,
    cellKey TEXT NOT NULL UNIQUE,
    signalStrength INTEGER NOT NULL,
    operator TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    count INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_cell_timestamp ON CellScanRecord(timestamp);
CREATE INDEX idx_cell_networkType ON CellScanRecord(networkType);
```

查询（与 WiFi/蓝牙对齐）：

- `selectAllCellRecords`: `SELECT * FROM CellScanRecord ORDER BY timestamp DESC;`
- `selectCellByCellKey`: `SELECT * FROM CellScanRecord WHERE cellKey = ?;`
- `selectCellRecordsPaginated`: 按 timestamp DESC 分页。
- `selectCellRecordsPaginatedBySignal`: 按 signalStrength DESC 分页。
- `countCellRecords`: `SELECT COUNT(*) FROM CellScanRecord;`
- `sumCellCount`: `SELECT IFNULL(SUM(count), 0) FROM CellScanRecord;`
- `insertCellRecord`: 插入全部字段。
- `updateCellRecord`: `UPDATE ... SET count = count + 1, networkType=?, signalStrength=?,
  operator=?, timestamp=?, latitude=(保留有效坐标), longitude=(保留有效坐标) WHERE cellKey = ?;`
- `deleteAllCellRecords`: 清空。

`cellKey` 唯一键由上层计算：`"$networkType-$mcc-$mnc-$lac-$cid"`（统一空白/未知处理）。

### models/CellScanRecord.kt

```kotlin
data class CellScanRecord(
    val id: Long = 0,
    val networkType: String,   // LTE / WCDMA / GSM / NR / CDMA
    val mcc: Int,
    val mnc: Int,
    val lac: Long,             // LAC 或 TAC
    val cid: Long,             // CID 或 CI
    val cellKey: String,
    val signalStrength: Int,   // dBm
    val operator: String,      // 运营商名 或 "Unknown"
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val count: Int = 1
)
```

### database/CellScanDao.kt

复用 `WifiScanDao` 的结构：`insertOrUpdate`（按 cellKey upsert，已存在则 count+1 并刷新字段，
无效坐标时保留原值）、`insertBatch`、`getAllRecords`、`getRecordByCellKey`、
`getRecordsPaginatedOrderedBySignal`、`getCount`、`getSeenTotal`、`deleteAll`。
所有挂起函数包在 `withContext(DatabaseFactory.dbDispatcher)` 中。

## 扫描实现（Android）

### platform/AndroidCellScanner.kt

- 构造接收 `Context`，内部获取 `TelephonyManager`。
- `suspend fun scanCells(): List<CellScanRecord>`：
  - 调用 `telephonyManager.allCellInfo`（需 `READ_PHONE_STATE` + 定位权限，否则返回空）。
  - 遍历 `CellInfo`，按类型抽取：
    - `CellInfoLte` → networkType="LTE", 取 `cellIdentity.mccString/mncString/lac/ci`，
      `cellSignalStrength.dbm`。
    - `CellInfoWcdma` → "WCDMA", `lac/cid`, dbm。
    - `CellInfoGsm` → "GSM", `lac/cid`, dbm。
    - `CellInfoNr` → "NR", 取 `cellIdentity.nrArfcn`/pci 映射为 lac/cid 占位，dbm。
    - `CellInfoCdma` → "CDMA", dbm。
  - mcc/mnc 优先用字符串字段（`mccString`），解析失败回退到数值字段。
  - operator 取 `telephonyManager.networkOperatorName`（或 `simOperatorName`）。
  - 统一生成 `cellKey`，过滤掉无法解析的条目。
- 对无法获得经纬度的单元，latitude/longitude 传 0.0，由落库时统一处理坐标（与 WiFi/蓝牙一致：
  扫描时若 `locationService.getCurrentLocation()` 为 null 则用 0.0，且更新时不覆盖已有有效坐标）。

### service/BackgroundScanService.kt

在 `startScanning()` 的 `scanningJob` 循环中，复用已有的 `location` 与 `database`：
- 调用 `AndroidCellScanner(context).scanCells()` 得到列表；
- 为每个单元附上当前 `location`（0.0 兜底）；
- `CellScanDao(database).insertBatch(recordsWithLocation)`；
- 包在 `try/catch` 中，失败仅打日志（与 WiFi 分支一致）。

### service/AndroidScannerService.kt

在 `startAllScans()` 的 `scanJob` 循环中加入同样的基站轮询与落库（复用 `cellDao`），
并包在已有的 `try/catch` 中。保持与 WiFi 分支一致。

## UI 集成（commonMain）

### ui/ScannerPage.kt

- `refreshData()` 中额外 `CellScanDao.getCount()` 得到 `cellCount`，新增 `private var cellCount by observable(0L)`；
  统计区可展示 Cell 数量（在现有 MdcCardRow 增加一项，或在文案中体现）。
- `buildMergedDevices`：读取 `cellDao.getRecordsPaginated(limit=20, offset=0)`，
  映射为 `MergedDevice(type="cell", title=operator/网络类型, identity=cellKey,
  primaryMetric="$signalStrength dBm", secondaryMetric=networkType, count=count, ...)`。

### ui/DeviceListPage.kt

- 新增 `MdcTab("Cell", ...)` 过滤，deviceFilter 增加 "cell"。
- `loadInitial`/`loadMore`：增加 `CellScanDao` 分页查询（按信号降序），合并进 `loadedItems`。
- `rebuildDisplay`：过滤条件支持 `it.type == "cell"`。
- `CellScanRecord.toDeviceItem()`：type="cell"，title=operator 或 "$networkType Cell"，
  identity=cellKey，primaryMetric="$signalStrength dBm"，secondaryMetric=networkType，
  displaySignal=signalStrength，key=cellKey。
- `openDeviceDetail`：当 type=="cell" 时传 `deviceType="cell"`、`deviceKey=cellKey`。
- 统计卡：可选增加 Cell 设备数/可见数展示（保持简洁，新增一项即可）。

### ui/DeviceDetailPage.kt

- `loadDevice()`：当 `deviceType == "cell"` 时调用 `CellScanDao(db).getRecordByCellKey(deviceKey)`，
  并 `renderCell(record, locations)`。
- `renderCell`：展示 网络类型 / MCC / MNC / LAC / CID / 信号 / 运营商 / 出现次数 / 时间戳；
  使用 `resolveLocation(record.latitude, record.longitude, locations)` 计算有效坐标，
  复用现有 `locationText` / `updateMapPreview` / `currentLat` / `currentLon` 逻辑，使地图正常显示。
- 记录为空时显示 "Device not found"。

### ui/StatisticsPage.kt

- 新增 Cell 统计卡（`totalCell by observable(0L)`，`cellDao.getCount()`）。
- 新增 "Top Cell" 区块：`cellDao.getAllRecords().sortedByDescending { it.count }.take(5)`，
  用 `MdcRankingRow` 展示（名称用 operator/网络类型，count 为出现次数）。

### ui/MapPage.kt

- 不改（继续展示 LocationRecord 采样点）。

## 权限

- 检查 `androidApp/src/main/AndroidManifest.xml`：
  - 确保已有 `ACCESS_FINE_LOCATION`（Android 10+ 必需，`getAllCellInfo` 才返回数据）。
  - 新增 `READ_PHONE_STATE`（如缺失）。
- 运行时权限：应用已有定位权限流程（AndroidLocationTracker），基站扫描复用同一权限；
  不做额外权限申请弹窗（保持与 WiFi/蓝牙一致）。若未授权，基站扫描返回空列表，不崩溃。

## 验证

- 按 `agent.md`：不执行编译/构建；完成改动后更新 `CHANGELOG.md` 并提交。
- 手动验收要点（由用户在设备上确认）：
  - 授权定位后开始扫描，ScannerPage 出现 Cell 条目、DeviceListPage 出现 Cell tab 与条目。
  - 点击 Cell 条目进入详情，地图正常显示（有坐标时）；无坐标时显示"无有效坐标"。
  - StatisticsPage 显示 Cell 统计与 Top Cell。
  - 长时间扫描不崩溃（数据库单线程调度 + try/catch 兜底）。

## 风险与备注

- iOS/ohos 不实现真实基站扫描；commonMain 的 `CellScanDao`/模型可在这些平台正常编译，
  只是不会被填充数据（无后台扫描调用）。
- `getAllCellInfo()` 在部分设备/权限不足时返回空列表，属正常情况，需保证不抛异常。
- 坐标回退逻辑与 WiFi/蓝牙共用 `DeviceDetailPage.resolveLocation`，确保无 GPS 时地图仍可基于
  最近 LocationRecord 显示。
