# Task 3: 集成SQLDelight数据库

## 任务描述

集成SQLDelight数据库，包括：
- 添加SQLDelight依赖
- 创建SQL Schema
- 创建数据库驱动接口
- 创建数据库工厂
- 创建WiFi扫描DAO
- 创建蓝牙扫描DAO
- 创建位置DAO

## 具体步骤

### Step 1: 添加SQLDelight依赖

在`shared/build.gradle.kts`中添加：
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
    id("app.cash.sqldelight") version "2.0.1"
}

sqldelight {
    databases {
        create("ScanAppDatabase") {
            packageName.set("com.example.scanapp.db")
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "com.tencent.kuikly:core-ksp:2.0.0")
    add("commonMainImplementation", "app.cash.sqldelight:coroutines-extensions:2.0.1")
}
```

### Step 2: 创建SQL Schema

```sql
-- shared/src/commonMain/sqldelight/com/example/scanapp/Database.sq
CREATE TABLE WifiScanRecord (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ssid TEXT NOT NULL,
    bssid TEXT NOT NULL,
    signalStrength INTEGER NOT NULL,
    frequency INTEGER NOT NULL,
    timestamp INTEGER NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    count INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE BluetoothScanRecord (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    address TEXT NOT NULL,
    rssi INTEGER NOT NULL,
    deviceType TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    count INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE LocationRecord (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    altitude REAL NOT NULL,
    accuracy REAL NOT NULL,
    timestamp INTEGER NOT NULL
);

-- 查询所有WiFi记录
selectAllWifiRecords:
SELECT * FROM WifiScanRecord ORDER BY timestamp DESC;

-- 根据BSSID查询WiFi记录
selectWifiByBssid:
SELECT * FROM WifiScanRecord WHERE bssid = ?;

-- 查询所有蓝牙记录
selectAllBluetoothRecords:
SELECT * FROM BluetoothScanRecord ORDER BY timestamp DESC;

-- 根据MAC地址查询蓝牙记录
selectBluetoothByAddress:
SELECT * FROM BluetoothScanRecord WHERE address = ?;

-- 查询所有位置记录
selectAllLocationRecords:
SELECT * FROM LocationRecord ORDER BY timestamp DESC;

-- 插入WiFi记录
insertWifiRecord:
INSERT INTO WifiScanRecord (ssid, bssid, signalStrength, frequency, timestamp, latitude, longitude, count)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

-- 更新WiFi记录计数
updateWifiCount:
UPDATE WifiScanRecord SET count = count + 1 WHERE bssid = ?;

-- 插入蓝牙记录
insertBluetoothRecord:
INSERT INTO BluetoothScanRecord (name, address, rssi, deviceType, timestamp, latitude, longitude, count)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

-- 更新蓝牙记录计数
updateBluetoothCount:
UPDATE BluetoothScanRecord SET count = count + 1 WHERE address = ?;

-- 插入位置记录
insertLocationRecord:
INSERT INTO LocationRecord (latitude, longitude, altitude, accuracy, timestamp)
VALUES (?, ?, ?, ?, ?);

-- 删除所有记录
deleteAllWifiRecords:
DELETE FROM WifiScanRecord;

deleteAllBluetoothRecords:
DELETE FROM BluetoothScanRecord;

deleteAllLocationRecords:
DELETE FROM LocationRecord;
```

### Step 3: 创建数据库驱动接口

```kotlin
// shared/src/commonMain/kotlin/com/example/scanapp/database/DatabaseDriver.kt
package com.example.scanapp.database

import app.cash.sqldelight.db.SqlDriver

expect fun createSqlDriver(): SqlDriver
```

### Step 4: 创建数据库工厂

```kotlin
// shared/src/commonMain/kotlin/com/example/scanapp/database/DatabaseFactory.kt
package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase

object DatabaseFactory {
    private var instance: ScanAppDatabase? = null
    
    fun getDatabase(): ScanAppDatabase {
        return instance ?: synchronized(this) {
            instance ?: createDatabase().also { instance = it }
        }
    }
    
    private fun createDatabase(): ScanAppDatabase {
        val driver = createSqlDriver()
        return ScanAppDatabase(driver)
    }
}
```

### Step 5: 创建WiFi扫描DAO

```kotlin
// shared/src/commonMain/kotlin/com/example/scanapp/database/WifiScanDao.kt
package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.models.WifiScanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WifiScanDao(private val database: ScanAppDatabase) {
    
    suspend fun insertOrUpdate(record: WifiScanRecord) = withContext(Dispatchers.Default) {
        val existing = database.scanAppQueries.selectWifiByBssid(record.bssid).executeAsOneOrNull()
        
        if (existing != null) {
            database.scanAppQueries.updateWifiCount(record.bssid)
        } else {
            database.scanAppQueries.insertWifiRecord(
                ssid = record.ssid,
                bssid = record.bssid,
                signalStrength = record.signalStrength,
                frequency = record.frequency,
                timestamp = record.timestamp,
                latitude = record.latitude,
                longitude = record.longitude,
                count = record.count
            )
        }
    }
    
    suspend fun getAllRecords(): List<WifiScanRecord> = withContext(Dispatchers.Default) {
        database.scanAppQueries.selectAllWifiRecords().executeAsList().map {
            WifiScanRecord(
                id = it.id,
                ssid = it.ssid,
                bssid = it.bssid,
                signalStrength = it.signalStrength,
                frequency = it.frequency,
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count
            )
        }
    }
    
    suspend fun deleteAll() = withContext(Dispatchers.Default) {
        database.scanAppQueries.deleteAllWifiRecords()
    }
}
```

### Step 6: 创建蓝牙扫描DAO

```kotlin
// shared/src/commonMain/kotlin/com/example/scanapp/database/BluetoothScanDao.kt
package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.models.BluetoothScanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BluetoothScanDao(private val database: ScanAppDatabase) {
    
    suspend fun insertOrUpdate(record: BluetoothScanRecord) = withContext(Dispatchers.Default) {
        val existing = database.scanAppQueries.selectBluetoothByAddress(record.address).executeAsOneOrNull()
        
        if (existing != null) {
            database.scanAppQueries.updateBluetoothCount(record.address)
        } else {
            database.scanAppQueries.insertBluetoothRecord(
                name = record.name,
                address = record.address,
                rssi = record.rssi,
                deviceType = record.deviceType,
                timestamp = record.timestamp,
                latitude = record.latitude,
                longitude = record.longitude,
                count = record.count
            )
        }
    }
    
    suspend fun getAllRecords(): List<BluetoothScanRecord> = withContext(Dispatchers.Default) {
        database.scanAppQueries.selectAllBluetoothRecords().executeAsList().map {
            BluetoothScanRecord(
                id = it.id,
                name = it.name,
                address = it.address,
                rssi = it.rssi,
                deviceType = it.deviceType,
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count
            )
        }
    }
    
    suspend fun deleteAll() = withContext(Dispatchers.Default) {
        database.scanAppQueries.deleteAllBluetoothRecords()
    }
}
```

### Step 7: 创建位置DAO

```kotlin
// shared/src/commonMain/kotlin/com/example/scanapp/database/LocationDao.kt
package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.models.LocationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationDao(private val database: ScanAppDatabase) {
    
    suspend fun insert(record: LocationRecord) = withContext(Dispatchers.Default) {
        database.scanAppQueries.insertLocationRecord(
            latitude = record.latitude,
            longitude = record.longitude,
            altitude = record.altitude,
            accuracy = record.accuracy,
            timestamp = record.timestamp
        )
    }
    
    suspend fun getAllRecords(): List<LocationRecord> = withContext(Dispatchers.Default) {
        database.scanAppQueries.selectAllLocationRecords().executeAsList().map {
            LocationRecord(
                id = it.id,
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude,
                accuracy = it.accuracy,
                timestamp = it.timestamp
            )
        }
    }
    
    suspend fun deleteAll() = withContext(Dispatchers.Default) {
        database.scanAppQueries.deleteAllLocationRecords()
    }
}
```

### Step 8: 提交代码

```bash
git add shared/
git commit -m "feat: 集成SQLDelight数据库，定义数据模型和DAO"
```

## 接口

- Consumes: Task 2的数据模型
- Produces: 数据库操作接口，供扫描和导出模块使用

## 全局约束

- 目标平台：Android 8.0+、iOS 13.0+、鸿蒙2.0+
- 使用Kuikly KMP工程模板
- 数据库使用SQLDelight
- UI使用Kuikly DSL构建
- 后台扫描需要平台特定的后台任务管理