# 基站扫描功能（Cell Station Scanning）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Android 上通过 `TelephonyManager.getAllCellInfo()` 定时轮询基站，并作为第三种设备类型（cell）与 WiFi/蓝牙统一存储、展示与查看。

**Architecture:** 复用现有 WiFi/蓝牙分层：SQLDelight 表 + `CellScanRecord` 模型 + `CellScanDao`（统一走 `DatabaseFactory.dbDispatcher` 单线程调度）；Android 平台新增 `AndroidCellScanner` 封装 `TelephonyManager`，在 `BackgroundScanService` 与 `AndroidScannerService` 的扫描循环中落库；UI 在 ScannerPage / DeviceListPage / DeviceDetailPage / StatisticsPage 中以 `type="cell"` 接入。

**Tech Stack:** Kotlin Multiplatform + Kuikly UI + SQLDelight + Android TelephonyManager。

## Global Constraints

- 项目规则（agent.md）：写完代码后**不执行 gradle 编译/构建验证**；每个任务完成代码后更新 `CHANGELOG.md` 并提交即可。
- 平台：仅 Android 实现真实基站扫描；iOS/ohos 不需改动即可编译通过。
- 数据库访问统一包在 `withContext(DatabaseFactory.dbDispatcher)`（单线程调度），禁止新增并发访问路径。
- 所有后台扫描写入必须包在 `try/catch` 中，失败仅打日志，不得让异常冒泡崩溃。
- 更新记录时，若传入坐标为无效值（NaN/Inf 或 0.0/0.0），保留数据库中已有有效坐标，不得覆盖。
- 命名/目录遵循现有结构：`commonMain` 放模型/DAO/UI，`androidMain` 放平台扫描实现。
- 坐标回退复用 `DeviceDetailPage.resolveLocation(record.latitude, record.longitude, locations)`。

---

### Task 1: SQLDelight 新增 CellScanRecord 表与查询

**Files:**
- Modify: `shared/src/commonMain/sqldelight/com/example/scanapp/Database.sq`（在 `idx_bluetooth_deviceType` 之后、`CREATE TABLE LocationRecord` 之前插入）

**Interfaces:**
- Produces: 生成的 `ScanAppDatabase` 查询方法 `selectAllCellRecords` / `selectCellByCellKey` / `selectCellRecordsPaginated` / `selectCellRecordsPaginatedBySignal` / `countCellRecords` / `sumCellCount` / `insertCellRecord` / `updateCellRecord` / `deleteAllCellRecords`，供 Task 3 使用。

- [ ] **Step 1: 在 Database.sq 插入 CellScanRecord 表、索引与查询**

在 `CREATE INDEX idx_bluetooth_deviceType ON BluetoothScanRecord(deviceType);` 之后、`CREATE TABLE LocationRecord (` 之前插入：

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

selectAllCellRecords:
SELECT * FROM CellScanRecord ORDER BY timestamp DESC;

selectCellByCellKey:
SELECT * FROM CellScanRecord WHERE cellKey = ?;

selectCellRecordsPaginated:
SELECT * FROM CellScanRecord ORDER BY timestamp DESC LIMIT :limit OFFSET :offset;

selectCellRecordsPaginatedBySignal:
SELECT * FROM CellScanRecord ORDER BY signalStrength DESC LIMIT :limit OFFSET :offset;

countCellRecords:
SELECT COUNT(*) FROM CellScanRecord;

sumCellCount:
SELECT IFNULL(SUM(count), 0) FROM CellScanRecord;

insertCellRecord:
INSERT INTO CellScanRecord (networkType, mcc, mnc, lac, cid, cellKey, signalStrength, operator, timestamp, latitude, longitude, count)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

updateCellRecord:
UPDATE CellScanRecord
SET count = count + 1,
    networkType = ?,
    signalStrength = ?,
    operator = ?,
    timestamp = ?,
    latitude = ?,
    longitude = ?
WHERE cellKey = ?;

deleteAllCellRecords:
DELETE FROM CellScanRecord;
```

- [ ] **Step 2: 提交**

```bash
git add shared/src/commonMain/sqldelight/com/example/scanapp/Database.sq
git commit -m "feat(db): add CellScanRecord table and queries"
```

---

### Task 2: CellScanRecord 模型与 cellKey 生成函数

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/models/CellScanRecord.kt`

**Interfaces:**
- Produces: `data class CellScanRecord(...)` 与顶层函数 `cellKeyOf(networkType, mcc, mnc, lac, cid): String`，供 Task 3/4/8/9/10 使用。

- [ ] **Step 1: 创建模型文件**

```kotlin
package com.example.scanapp.models

import kotlinx.serialization.Serializable

@Serializable
data class CellScanRecord(
    val id: Long = 0,
    val networkType: String,
    val mcc: Int,
    val mnc: Int,
    val lac: Long,
    val cid: Long,
    val cellKey: String,
    val signalStrength: Int,
    val operator: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val count: Int = 1
)

fun cellKeyOf(networkType: String, mcc: Int, mnc: Int, lac: Long, cid: Long): String =
    "$networkType-$mcc-$mnc-$lac-$cid"
```

- [ ] **Step 2: 提交**

```bash
git add shared/src/commonMain/kotlin/com/example/scanapp/models/CellScanRecord.kt
git commit -m "feat(model): add CellScanRecord and cellKeyOf helper"
```

---

### Task 3: CellScanDao

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/database/CellScanDao.kt`

**Interfaces:**
- Consumes: Task 1 生成的 `database.databaseQueries.*`；`DatabaseFactory.dbDispatcher`；Task 2 的 `CellScanRecord` + `cellKeyOf`。
- Produces: `CellScanDao`（`insertOrUpdate` / `insertBatch` / `getAllRecords` / `getRecordByCellKey` / `getRecordsPaginatedOrderedBySignal` / `getCount` / `getSeenTotal` / `deleteAll`），供 Task 5/6 与 Task 8/9/10 使用。

- [ ] **Step 1: 创建 DAO 文件**

```kotlin
package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.models.CellScanRecord
import kotlinx.coroutines.withContext

class CellScanDao(private val database: ScanAppDatabase) {

    private fun mapRow(it: com.example.scanapp.db.CellScanRecord): CellScanRecord = CellScanRecord(
        id = it.id,
        networkType = it.networkType,
        mcc = it.mcc.toInt(),
        mnc = it.mnc.toInt(),
        lac = it.lac,
        cid = it.cid,
        cellKey = it.cellKey,
        signalStrength = it.signalStrength.toInt(),
        operator = it.operator,
        timestamp = it.timestamp,
        latitude = it.latitude,
        longitude = it.longitude,
        count = it.count.toInt()
    )

    suspend fun insertOrUpdate(record: CellScanRecord) = withContext(DatabaseFactory.dbDispatcher) {
        database.transaction { insertOrUpdateInTransaction(record) }
    }

    suspend fun insertBatch(records: List<CellScanRecord>) = withContext(DatabaseFactory.dbDispatcher) {
        database.transaction { records.forEach { insertOrUpdateInTransaction(it) } }
    }

    suspend fun getAllRecords(): List<CellScanRecord> = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.selectAllCellRecords().executeAsList().map { mapRow(it) }
    }

    suspend fun getRecordByCellKey(cellKey: String): CellScanRecord? = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.selectCellByCellKey(cellKey).executeAsOneOrNull()?.let { mapRow(it) }
    }

    suspend fun getRecordsPaginatedOrderedBySignal(limit: Int, offset: Int): List<CellScanRecord> =
        withContext(DatabaseFactory.dbDispatcher) {
            database.databaseQueries.selectCellRecordsPaginatedBySignal(limit.toLong(), offset.toLong())
                .executeAsList().map { mapRow(it) }
        }

    suspend fun getCount(): Long = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.countCellRecords().executeAsOne()
    }

    suspend fun getSeenTotal(): Long = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.sumCellCount().executeAsOne().IFNULL ?: 0L
    }

    suspend fun deleteAll() = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.deleteAllCellRecords()
    }

    private fun insertOrUpdateInTransaction(record: CellScanRecord) {
        val existing = database.databaseQueries.selectCellByCellKey(record.cellKey).executeAsOneOrNull()
        if (existing == null) {
            database.databaseQueries.insertCellRecord(
                networkType = record.networkType,
                mcc = record.mcc.toLong(),
                mnc = record.mnc.toLong(),
                lac = record.lac,
                cid = record.cid,
                cellKey = record.cellKey,
                signalStrength = record.signalStrength.toLong(),
                operator = record.operator,
                timestamp = record.timestamp,
                latitude = record.latitude,
                longitude = record.longitude,
                count = record.count.toLong()
            )
        } else {
            val invalid = record.latitude.isNaN() || record.latitude.isInfinite() ||
                record.longitude.isNaN() || record.longitude.isInfinite() ||
                (record.latitude == 0.0 && record.longitude == 0.0)
            database.databaseQueries.updateCellRecord(
                networkType = record.networkType,
                signalStrength = record.signalStrength.toLong(),
                operator = record.operator,
                timestamp = record.timestamp,
                latitude = if (invalid) existing.latitude else record.latitude,
                longitude = if (invalid) existing.longitude else record.longitude,
                cellKey = record.cellKey
            )
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add shared/src/commonMain/kotlin/com/example/scanapp/database/CellScanDao.kt
git commit -m "feat(db): add CellScanDao"
```

---

### Task 4: AndroidCellScanner（androidMain）

**Files:**
- Create: `shared/src/androidMain/kotlin/com/example/scanapp/platform/AndroidCellScanner.kt`

**Interfaces:**
- Consumes: Task 2 的 `CellScanRecord` + `cellKeyOf`。
- Produces: `AndroidCellScanner(context).scanCells(): List<CellScanRecord>`，供 Task 5/6 使用。

- [ ] **Step 1: 创建 AndroidCellScanner**

```kotlin
package com.example.scanapp.platform

import android.content.Context
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrength
import android.telephony.TelephonyManager
import com.example.scanapp.models.CellScanRecord
import com.example.scanapp.models.cellKeyOf

class AndroidCellScanner(private val context: Context) {
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    fun scanCells(): List<CellScanRecord> {
        val operator = runCatching { telephonyManager.networkOperatorName?.toString() }
            .getOrNull().takeIf { !it.isNullOrBlank() } ?: "Unknown"
        val raw = runCatching { telephonyManager.allCellInfo }.getOrNull() ?: return emptyList()
        val now = System.currentTimeMillis()
        return raw.mapNotNull { toRecord(it, operator, now) }
    }

    private fun toRecord(info: CellInfo, operator: String, now: Long): CellScanRecord? {
        val dbm = signalDbm(info) ?: return null
        val id = identity(info) ?: return null
        if (id.mcc <= 0 || id.mnc <= 0 || id.lac < 0 || id.cid < 0) return null
        return CellScanRecord(
            networkType = id.type,
            mcc = id.mcc,
            mnc = id.mnc,
            lac = id.lac,
            cid = id.cid,
            cellKey = cellKeyOf(id.type, id.mcc, id.mnc, id.lac, id.cid),
            signalStrength = dbm,
            operator = operator,
            timestamp = now,
            latitude = 0.0,
            longitude = 0.0,
            count = 1
        )
    }

    private fun signalDbm(info: CellInfo): Int? {
        val ss = info.cellSignalStrength ?: return null
        val dbm = ss.dbm
        if (dbm == CellSignalStrength.INVALID || dbm == Int.MAX_VALUE || dbm >= 0) return null
        return dbm
    }

    private data class CellIdentityData(
        val type: String, val mcc: Int, val mnc: Int, val lac: Long, val cid: Long
    )

    // 归一化：未知值（<=0 或平台哨兵 Integer.MAX_VALUE）统一记为 -1，最终被 toRecord 过滤丢弃
    private fun norm(v: Int): Int = if (v <= 0 || v == Int.MAX_VALUE) -1 else v

    private fun identity(info: CellInfo): CellIdentityData? = when (info) {
        is CellInfoLte -> {
            val id = info.cellIdentity
            CellIdentityData(
                "LTE",
                norm(id.mccString?.toIntOrNull() ?: id.mcc),
                norm(id.mncString?.toIntOrNull() ?: id.mnc),
                norm(id.lac).toLong(),
                norm(id.ci).toLong()
            )
        }
        is CellInfoWcdma -> {
            val id = info.cellIdentity
            CellIdentityData(
                "WCDMA",
                norm(id.mccString?.toIntOrNull() ?: id.mcc),
                norm(id.mncString?.toIntOrNull() ?: id.mnc),
                norm(id.lac).toLong(),
                norm(id.cid).toLong()
            )
        }
        is CellInfoGsm -> {
            val id = info.cellIdentity
            CellIdentityData(
                "GSM",
                norm(id.mccString?.toIntOrNull() ?: id.mcc),
                norm(id.mncString?.toIntOrNull() ?: id.mnc),
                norm(id.lac).toLong(),
                norm(id.cid).toLong()
            )
        }
        is CellInfoCdma -> {
            val id = info.cellIdentity
            CellIdentityData("CDMA", -1, -1, norm(id.networkId).toLong(), norm(id.systemId).toLong())
        }
        is CellInfoNr -> {
            val id = info.cellIdentity as? android.telephony.CellIdentityNr ?: return null
            CellIdentityData(
                "NR",
                norm(id.mccString?.toIntOrNull() ?: -1),
                norm(id.mncString?.toIntOrNull() ?: -1),
                norm(id.nrarfcn).toLong(),
                norm(id.pci).toLong()
            )
        }
        else -> null
    }
}

private fun String.toIntOrNull(): Int? = runCatching { toInt() }.getOrNull()
```

- [ ] **Step 2: 提交**

```bash
git add shared/src/androidMain/kotlin/com/example/scanapp/platform/AndroidCellScanner.kt
git commit -m "feat(android): add AndroidCellScanner wrapping TelephonyManager"
```

---

### Task 5: BackgroundScanService 接入基站轮询

**Files:**
- Modify: `shared/src/androidMain/kotlin/com/example/scanapp/service/BackgroundScanService.kt`

**Interfaces:**
- Consumes: Task 3 的 `CellScanDao`；Task 4 的 `AndroidCellScanner`。
- Produces: 后台扫描主循环在保存 WiFi 后额外保存基站记录。

- [ ] **Step 1: 新增字段与导入**

在 `BackgroundScanService` 顶部 `import` 区增加：

```kotlin
import com.example.scanapp.database.CellScanDao
import com.example.scanapp.platform.AndroidCellScanner
```

在 `private lateinit var bluetoothScanner: AndroidBluetoothScanner` 附近增加：

```kotlin
private lateinit var cellScanner: AndroidCellScanner
```

在 `onCreate()` 的 `bluetoothScanner = AndroidBluetoothScanner(this)` 之后增加：

```kotlin
cellScanner = AndroidCellScanner(this)
```

- [ ] **Step 2: 在扫描循环中保存基站**

在 `scanningJob = scope.launch { while (isActive) { ... } }` 中，WiFi 保存的 `try/catch` 块之后、`delay(scanInterval)` 之前，插入：

```kotlin
try {
    val cellResults = cellScanner.scanCells()
    val cellRecords = cellResults.map { rec ->
        rec.copy(latitude = location?.latitude ?: 0.0, longitude = location?.longitude ?: 0.0)
    }
    if (cellRecords.isNotEmpty()) {
        CellScanDao(database).insertBatch(cellRecords)
    }
} catch (e: Exception) {
    android.util.Log.e("BackgroundScanService", "Cell save error: ${e.message}")
}
```

- [ ] **Step 3: 提交**

```bash
git add shared/src/androidMain/kotlin/com/example/scanapp/service/BackgroundScanService.kt
git commit -m "feat(scan): poll and persist cell towers in background scan"
```

---

### Task 6: AndroidScannerService（WorkManager worker）接入基站轮询

**Files:**
- Modify: `shared/src/androidMain/kotlin/com/example/scanapp/service/AndroidScannerService.kt`

**Interfaces:**
- Consumes: Task 3 的 `CellScanDao`；Task 4 的 `AndroidCellScanner`。
- Produces: worker 扫描循环额外保存基站记录。

- [ ] **Step 1: 新增字段与导入**

在 `import` 区增加：

```kotlin
import com.example.scanapp.database.CellScanDao
import com.example.scanapp.platform.AndroidCellScanner
```

在 `private val bluetoothScanner = AndroidBluetoothScanner(context)` 附近增加：

```kotlin
private val cellScanner = AndroidCellScanner(context)
```

- [ ] **Step 2: 在 startAllScans 循环保存基站**

在 `startAllScans()` 的 `scanJob = scope.launch { while (true) { try { ...wifi... } catch {...} delay(5000) } }` 中，WiFi 保存之后、`delay(5000)` 之前插入：

```kotlin
try {
    val cellResults = cellScanner.scanCells()
    val cellRecords = cellResults.map { rec ->
        rec.copy(latitude = location?.latitude ?: 0.0, longitude = location?.longitude ?: 0.0)
    }
    if (cellRecords.isNotEmpty()) {
        bluetoothDao // touch to keep import order; actual dao below
        CellScanDao(database).insertBatch(cellRecords)
    }
} catch (e: Exception) {
    android.util.Log.e("AndroidScannerService", "Cell save error: ${e.message}")
}
```

注意：`database` 在该作用域内需可见；`startAllScans` 内已通过 `wifiDao` 持有 `database`（即 `WifiScanDao` 构造时传入的同一实例）。若作用域内无 `database` 变量，新增 `val database = DatabaseFactory.getDatabase()` 并传入 `CellScanDao(database)`。

- [ ] **Step 3: 提交**

```bash
git add shared/src/androidMain/kotlin/com/example/scanapp/service/AndroidScannerService.kt
git commit -m "feat(scan): poll and persist cell towers in worker scan"

---

# Part B — UI 集成（Task 7–12）

> 以下改动全部在 `shared/src/commonMain`，UI 框架为 Kuikly。
> 约定：设备 `type` 取值新增 `"cell"`，`key` = `cellKey`（见 Task 2）。
> 数据流沿用 `observableList` / `observable` + `vfor` / `vif` / `safeValue` 等既有封装。

## Task 7 — ScannerPage：新增基站计数 + 合并到设备流

文件：`shared/src/commonMain/kotlin/com/example/scanapp/ui/ScannerPage.kt`

1. 新增字段与 import：
```kotlin
import com.example.scanapp.models.CellScanRecord
import com.example.scanapp.database.CellScanDao
// ... 已有 wifi / bluetooth 的 import

private var cellCount by observable(0L)
```

2. `refreshData(...)` 在 `val db = ...` 之后、`wifiDao`/`bluetoothDao` 附近新增：
```kotlin
val cellDao = CellScanDao(db)
cellCount = cellDao.getCount()
val cellRecords = cellDao.getRecordsPaginatedOrderedBySignal(limit = 20, offset = 0)
```
并在 `mergedDevices.diffUpdate(buildMergedDevices(...))` 处改为：
```kotlin
this@ScannerPage.mergedDevices.diffUpdate(
    buildMergedDevices(wifiRecords, bluetoothRecords, cellRecords)
)
```

3. `buildMergedDevices` 签名改为 `(wifi, bluetooth, cell)` 三参数，末尾追加 cell 映射：
```kotlin
private fun buildMergedDevices(
    wifi: List<WifiScanRecord>,
    bluetooth: List<BluetoothScanRecord>,
    cell: List<CellScanRecord>
): List<MergedDevice> {
    val list = mutableListOf<MergedDevice>()
    wifi.forEach { ... }      // 既有
    bluetooth.forEach { ... } // 既有
    cell.forEach {
        list.add(
            MergedDevice(
                type = "cell",
                title = if (it.operator.isNotEmpty() && it.operator != "Unknown") it.operator else "${it.networkType} Cell",
                identity = it.cellKey,
                primaryMetric = "${it.signalStrength} dBm",
                secondaryMetric = it.networkType,
                count = it.count,
                timestamp = it.timestamp,
                tag = "Cell",
                tagColor = MdcTheme.Colors.cell
            )
        )
    }
    return list
}
```

4. 顶部统计卡片 `MdcCardRow`（含 WiFi / Bluetooth 徽章处）新增 Cell 徽章：
```kotlin
MdcCardRow {
    MdcStatBadge("WiFi", { "${this@ScannerPage.wifiCount}" }, MdcTheme.Colors.wifi)
    MdcStatBadge("Bluetooth", { "${this@ScannerPage.bluetoothCount}" }, MdcTheme.Colors.bluetooth)
    MdcStatBadge("Cell", { "${this@ScannerPage.cellCount}" }, MdcTheme.Colors.cell)
}
```

5. 提交：
```bash
git add shared/src/commonMain/kotlin/com/example/scanapp/ui/ScannerPage.kt
git commit -m "feat(ui): show cell station count and merge into scanner device list"
```

## Task 8 — DeviceListPage：新增 Cell Tab + 分页 + 跳转

文件：`shared/src/commonMain/kotlin/com/example/scanapp/ui/DeviceListPage.kt`

1. import：
```kotlin
import com.example.scanapp.models.CellScanRecord
import com.example.scanapp.database.CellScanDao
```

2. 顶部 `MdcTab` 列表在 Bluetooth tab 之后新增 Cell tab：
```kotlin
MdcTab("Cell", { safeBool("DeviceList.tab") { this@DeviceListPage.deviceFilter == "cell" } }) {
    this@DeviceListPage.setFilter("cell")
}
```

3. 新增状态字段（与 `wifiTotal`/`bluetoothTotal` 同一区域）：
```kotlin
private var cellDeviceCount by observable(0)
private var cellSeenTotal by observable(0)
private var loadedCellCount = 0
private var cellTotal = 0
```

4. 第三个 `MdcCardRow`（统计徽章）新增 Cell 统计：
```kotlin
MdcCardRow(elevation = MdcTheme.Elevation.level0) {
    MdcStatBadge("Cell Devices", { safeValue("DeviceList.stat") { "${this@DeviceListPage.cellDeviceCount}" } }, MdcTheme.Colors.cell)
    MdcStatBadge("Cell Seen", { safeValue("DeviceList.stat") { "${this@DeviceListPage.cellSeenTotal}" } }, MdcTheme.Colors.cell)
}
```

5. `loadInitial()` 改为（在 `val db = ` 之后、`wifiDao`/`bluetoothDao` 旁新增 `cellDao`，并扩展读取/追加）：
```kotlin
val cellDao = CellScanDao(db)
wifiTotal = wifiDao.getCount().toInt()
bluetoothTotal = bluetoothDao.getCount().toInt()
cellTotal = cellDao.getCount().toInt()
wifiDeviceCount = wifiTotal
bluetoothDeviceCount = bluetoothTotal
cellDeviceCount = cellTotal
wifiSeenTotal = wifiDao.getSeenTotal().toInt()
bluetoothSeenTotal = bluetoothDao.getSeenTotal().toInt()
cellSeenTotal = cellDao.getSeenTotal().toInt()

val wifiPage = wifiDao.getRecordsPaginatedOrderedBySignal(pageSize, 0)
val bluetoothPage = bluetoothDao.getRecordsPaginatedOrderedByRssi(pageSize, 0)
val cellPage = cellDao.getRecordsPaginatedOrderedBySignal(pageSize, 0)
loadedWifiCount = wifiPage.size
loadedBluetoothCount = bluetoothPage.size
loadedCellCount = cellPage.size

loadedItems.clear()
loadedItems.addAll(wifiPage.map { it.toDeviceItem() })
loadedItems.addAll(bluetoothPage.map { it.toDeviceItem() })
loadedItems.addAll(cellPage.map { it.toDeviceItem() })
```

6. `hasMore()` 追加条件：
```kotlin
loadedWifiCount < wifiTotal || loadedBluetoothCount < bluetoothTotal || loadedCellCount < cellTotal
```

7. `loadMore()` 在声明 `wifiDao`/`bluetoothDao` 处补 `val cellDao = CellScanDao(db)`，并扩展：
```kotlin
val cellPage = if (loadedCellCount < cellTotal) {
    cellDao.getRecordsPaginatedOrderedBySignal(pageSize, loadedCellCount)
} else { emptyList() }

loadedWifiCount += wifiPage.size
loadedBluetoothCount += bluetoothPage.size
loadedCellCount += cellPage.size
loadedItems.addAll(wifiPage.map { it.toDeviceItem() })
loadedItems.addAll(bluetoothPage.map { it.toDeviceItem() })
loadedItems.addAll(cellPage.map { it.toDeviceItem() })
```
   `rebuildDisplay()` 的 `filter { deviceFilter == "all" || it.type == deviceFilter }` 已天然支持 `"cell"`，无需改动。

8. 新增 `CellScanRecord.toDeviceItem()`（与既有 wifi/bluetooth 扩展函数并列）：
```kotlin
private fun CellScanRecord.toDeviceItem() = DeviceItem(
    type = "cell",
    key = cellKey,
    firstLine = if (operator.isNotEmpty() && operator != "Unknown") operator else "$networkType Cell",
    secondLine = "$networkType · ${signalStrength}dBm · MCC $mcc MNC $mnc",
    rightTop = "$count",
    rightBottom = cellKey,
    tagColor = MdcTheme.Colors.cell
)
```

9. `openDeviceDetail` 已按 `deviceType = item.type`、`key = item.key` 转发，`type="cell"` / `key=cellKey` 可直接命中 Task 9 的分支，无需改动。

10. 提交：
```bash
git add shared/src/commonMain/kotlin/com/example/scanapp/ui/DeviceListPage.kt
git commit -m "feat(ui): add Cell tab with pagination and deep link in device list"
```

## Task 9 — DeviceDetailPage：新增 cell 详情分支

文件：`shared/src/commonMain/kotlin/com/example/scanapp/ui/DeviceDetailPage.kt`

1. import：
```kotlin
import com.example.scanapp.models.CellScanRecord
import com.example.scanapp.database.CellScanDao
```

2. `loadDevice()` 的 `if (deviceType == "wifi") {...} else {...}` 改为三段式：
```kotlin
if (deviceType == "wifi") {
    val record = WifiScanDao(db).getRecordByBssid(deviceKey)
    if (isPageActive) renderWifi(record, locations)
} else if (deviceType == "cell") {
    val record = CellScanDao(db).getRecordByCellKey(deviceKey)
    if (isPageActive) renderCell(record, locations)
} else {
    val record = BluetoothScanDao(db).getRecordByAddress(deviceKey)
    if (isPageActive) renderBluetooth(record, locations)
}
```

3. 新增 `renderCell`（结构对齐 `renderWifi`，坐标回退沿用 `resolveLocation(record?.latitude ?: 0.0, record?.longitude ?: 0.0)`）：
```kotlin
private fun renderCell(record: CellScanRecord?, locations: List<LocationRecord>) {
    val (lat, lng) = resolveLocation(record?.latitude ?: 0.0, record?.longitude ?: 0.0, locations)
    detailContent.diffUpdate(buildList {
        add(DeviceDetailSection("Base Station"))
        add(DeviceDetailRow("Network Type", record?.networkType ?: "-"))
        add(DeviceDetailRow("Operator", record?.operator ?: "-"))
        add(DeviceDetailRow("MCC / MNC", "${record?.mcc ?: "-"} / ${record?.mnc ?: "-"}"))
        add(DeviceDetailRow("LAC / TAC", "${record?.lac ?: "-"}"))
        add(DeviceDetailRow("CID / CI", "${record?.cid ?: "-"}"))
        add(DeviceDetailRow("Signal", "${record?.signalStrength ?: "-"} dBm"))
        add(DeviceDetailRow("Times Seen", "${record?.count ?: 0}"))
        add(DeviceDetailRow("Last Seen", formatTime(record?.timestamp ?: 0L)))
        add(DeviceDetailSection("Location"))
        add(DeviceDetailRow("Latitude", "%.6f".format(lat)))
        add(DeviceDetailRow("Longitude", "%.6f".format(lng)))
        add(DeviceDetailMapRow(lat, lng, record?.operator ?: "Cell"))
    })
}
```
   （`DeviceDetailMapRow`、`DeviceDetailRow`、`DeviceDetailSection`、`formatTime`、`resolveLocation` 均已在文件中存在，直接复用。）

4. 提交：
```bash
git add shared/src/commonMain/kotlin/com/example/scanapp/ui/DeviceDetailPage.kt
git commit -m "feat(ui): render cell station detail with resolved location"
```

## Task 10 — StatisticsPage：新增 Cell 统计 + Top Cell

文件：`shared/src/commonMain/kotlin/com/example/scanapp/ui/StatisticsPage.kt`

1. import：
```kotlin
import com.example.scanapp.models.CellScanRecord
import com.example.scanapp.database.CellScanDao
```

2. 新增状态：
```kotlin
private var totalCell by observable(0L)
private var topCell by observableList<CellScanRecord>()
```

3. `refreshData()` 新增：
```kotlin
val cellDao = CellScanDao(db)
totalCell = cellDao.getCount()
topCell = cellDao.getAllRecords().sortedByDescending { it.count }.take(5)
```

4. 顶部统计 `MdcCardRow` 新增 Cell 徽章（第三个 `MdcStatBadge`）：
```kotlin
MdcCardRow {
    MdcStatBadge("WiFi", { "${this@StatisticsPage.totalWifi}" }, MdcTheme.Colors.wifi)
    MdcStatBadge("BT", { "${this@StatisticsPage.totalBluetooth}" }, MdcTheme.Colors.bluetooth)
    MdcStatBadge("Cell", { "${this@StatisticsPage.totalCell}" }, MdcTheme.Colors.cell)
}
```

5. 新增 “Top Cell” 区块（放在 “Top Bluetooth” 区块之后）：
```kotlin
// Top Cell
MdcSectionTitle("Top Cell (by times seen)")
if (topCell.isEmpty()) {
    MdcEmptyHint("No cell records yet")
} else {
    vforIndexed(topCell) { index, record ->
        MdcRankRow(
            rank = index + 1,
            title = if (record.operator.isNotEmpty() && record.operator != "Unknown") record.operator else "${record.networkType} Cell",
            subtitle = "${record.networkType} · MCC ${record.mcc} MNC ${record.mnc}",
            metric = "${record.count} seen"
        )
    }
}
```
   （`MdcSectionTitle` / `MdcEmptyHint` / `MdcRankRow` / `vforIndexed` 均已存在，直接复用。）

6. `MdcTheme` 新增 cell 色：
   - 文件：`shared/src/commonMain/kotlin/com/example/scanapp/ui/MdcTheme.kt`
   - 在 `Colors` 对象 `val bluetooth = Color(0xff00897BL)` 之后新增：
```kotlin
        val cell = Color(0xff6750A4L)
```

7. 提交：
```bash
git add shared/src/commonMain/kotlin/com/example/scanapp/ui/StatisticsPage.kt shared/src/commonMain/kotlin/com/example/scanapp/ui/MdcTheme.kt
git commit -m "feat(ui): add cell statistics and top cell ranking"
```

## Task 11 — AndroidManifest：补充权限

文件：`androidApp/src/main/AndroidManifest.xml`

- 确认已存在 `android.permission.ACCESS_FINE_LOCATION`（后台扫描已用）；若不存在则补上。
- 在其它 `<uses-permission>` 旁新增读取基站信息所需权限：
```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```
  （`getAllCellInfo()` 在 Android 10+ 受 `READ_PHONE_STATE` 限制；若 targetSdk ≥ 31，可能还需 `READ_PHONE_NUMBERS`，但仅取小区信息通常 `READ_PHONE_STATE` 足够。本任务只补 `READ_PHONE_STATE`。）

提交：
```bash
git add androidApp/src/main/AndroidManifest.xml
git commit -m "chore(android): add READ_PHONE_STATE for cell info"
```

## Task 12 — CHANGELOG 更新

文件：`CHANGELOG.md`

在顶部「Unreleased」或最新条目下新增一条：
```
## [Unreleased]
### Added
- Cell (base station) scanning for Android via `TelephonyManager.getAllCellInfo()`, polled alongside WiFi/Bluetooth.
- Cell records stored in new `CellScanRecord` table; surfaced in Scanner, Device List (Cell tab), Device Detail, and Statistics (Top Cell).
```

提交：
```bash
git add CHANGELOG.md
git commit -m "docs: update CHANGELOG for cell station scanning"
```

---

# Verification（不编译，按 agent.md 规则）

- 本计划所有任务完成后，**不执行 gradle 编译/构建**。
- 每完成一个 Task 即 `git commit` 一次（见各 Task 末的提交命令）。
- 最终由人工在 Android 真机运行验证：开启后台扫描 → 查看 Scanner 页 Cell 计数与设备流、DeviceList Cell Tab 分页与详情、Statistics Top Cell。
- 数据库迁移：新增 `CellScanRecord` 表为全新表，不破坏既有 `WifiScanRecord` / `BluetoothScanRecord` 表，旧库可直接运行（SQLDelight 自动建表）。
```
