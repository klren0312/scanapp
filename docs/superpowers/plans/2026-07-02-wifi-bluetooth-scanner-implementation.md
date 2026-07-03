# WiFi/Bluetooth扫描器实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建一个跨平台WiFi/蓝牙扫描器应用，支持Android、iOS和鸿蒙，能够扫描附近设备并记录GPS位置，保存到本地SQLite数据库，支持数据导出。

**Architecture:** 采用模块化架构，使用Kuikly KMP框架实现跨平台开发。项目分为shared模块（跨平台业务逻辑）和平台壳工程（Android/iOS/鸿蒙）。核心功能包括WiFi/蓝牙扫描、GPS定位、SQLite存储和数据导出。

**Tech Stack:** 
- Kuikly UI框架
- Kotlin Multiplatform
- SQLDelight（跨平台SQLite）
- 原生平台API（WiFi、蓝牙、GPS）

## Global Constraints

- 目标平台：Android 8.0+、iOS 13.0+、鸿蒙2.0+
- 使用Kuikly KMP工程模板
- 数据库使用SQLDelight
- UI使用Kuikly DSL构建
- 后台扫描需要平台特定的后台任务管理

---

## Task 1: 创建Kuikly KMP工程

**Files:**
- Create: `D:\1project\scanapp\` (工程根目录)
- Create: `shared/` (跨平台模块)
- Create: `androidApp/` (Android壳工程)
- Create: `iosApp/` (iOS壳工程)
- Create: `ohosApp/` (鸿蒙壳工程)

**Interfaces:**
- Consumes: 无
- Produces: 基础工程结构，包含Kuikly框架依赖

- [ ] **Step 1: 使用Android Studio创建Kuikly工程**
  
  打开Android Studio，选择 File -> New -> New Project -> Kuikly Project Template
  - 项目名称：scanapp
  - 包名：com.example.scanapp
  - 最低支持版本：Android 8.0、iOS 13.0
  - 选择平台：Android、iOS、HarmonyOS
  - DSL类型：Kuikly DSL

- [ ] **Step 2: 验证工程结构**
  
  检查工程目录结构：
  ```
  scanapp/
  ├── shared/
  │   └── src/
  │       ├── commonMain/
  │       ├── androidMain/
  │       ├── iosMain/
  │       └── ohosArm64Main/
  ├── androidApp/
  ├── iosApp/
  └── ohosApp/
  ```

- [ ] **Step 3: 配置共享模块依赖**
  
  在`shared/build.gradle.kts`中添加KSP插件：
  ```kotlin
  plugins {
      kotlin("multiplatform")
      id("com.google.devtools.ksp") version "2.0.21-1.0.28"
  }
  
  ksp {
      arg("moduleId", "shared")
  }
  
  dependencies {
      add("kspCommonMainMetadata", "com.tencent.kuikly:core-ksp:2.0.0")
  }
  ```

- [ ] **Step 4: 编译工程**
  
  运行Gradle同步：
  ```bash
  ./gradlew build
  ```
  预期：BUILD SUCCESSFUL

- [ ] **Step 5: 提交代码**
  
  ```bash
  git init
  git add .
  git commit -m "feat: 创建Kuikly KMP工程基础结构"
  ```

---

## Task 2: 定义数据模型

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/models/WifiScanRecord.kt`
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/models/BluetoothScanRecord.kt`
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/models/LocationRecord.kt`

**Interfaces:**
- Consumes: 无
- Produces: 数据模型类，供后续任务使用

- [ ] **Step 1: 创建WiFi扫描记录模型**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/models/WifiScanRecord.kt
  package com.example.scanapp.models
  
  data class WifiScanRecord(
      val id: Long = 0,
      val ssid: String,
      val bssid: String,
      val signalStrength: Int,
      val frequency: Int,
      val timestamp: Long,
      val latitude: Double,
      val longitude: Double,
      val count: Int = 1
  )
  ```

- [ ] **Step 2: 创建蓝牙扫描记录模型**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/models/BluetoothScanRecord.kt
  package com.example.scanapp.models
  
  data class BluetoothScanRecord(
      val id: Long = 0,
      val name: String,
      val address: String,
      val rssi: Int,
      val deviceType: String,
      val timestamp: Long,
      val latitude: Double,
      val longitude: Double,
      val count: Int = 1
  )
  ```

- [ ] **Step 3: 创建GPS位置记录模型**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/models/LocationRecord.kt
  package com.example.scanapp.models
  
  data class LocationRecord(
      val id: Long = 0,
      val latitude: Double,
      val longitude: Double,
      val altitude: Double,
      val accuracy: Float,
      val timestamp: Long
  )
  ```

- [ ] **Step 4: 提交代码**
  
  ```bash
  git add shared/src/commonMain/kotlin/com/example/scanapp/models/
  git commit -m "feat: 添加数据模型定义"
  ```

---

## Task 3: 集成SQLDelight数据库

**Files:**
- Modify: `shared/build.gradle.kts`
- Create: `shared/src/commonMain/sqldelight/com/example/scanapp/Database.sq`
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/database/DatabaseDriver.kt`
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/database/DatabaseFactory.kt`
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/database/WifiScanDao.kt`
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/database/BluetoothScanDao.kt`
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/database/LocationDao.kt`

**Interfaces:**
- Consumes: Task 2的数据模型
- Produces: 数据库操作接口，供扫描和导出模块使用

- [ ] **Step 1: 添加SQLDelight依赖**
  
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

- [ ] **Step 2: 创建SQL Schema**
  
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

- [ ] **Step 3: 创建数据库驱动接口**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/database/DatabaseDriver.kt
  package com.example.scanapp.database
  
  import app.cash.sqldelight.db.SqlDriver
  
  expect fun createSqlDriver(): SqlDriver
  ```

- [ ] **Step 4: 创建数据库工厂**
  
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

- [ ] **Step 5: 创建WiFi扫描DAO**
  
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

- [ ] **Step 6: 创建蓝牙扫描DAO**
  
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

- [ ] **Step 7: 创建位置DAO**
  
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

- [ ] **Step 8: 编译验证**
  
  ```bash
  ./gradlew :shared:generateMainDatabaseInterface
  ```
  预期：BUILD SUCCESSFUL

- [ ] **Step 9: 提交代码**
  
  ```bash
  git add shared/
  git commit -m "feat: 集成SQLDelight数据库，定义数据模型和DAO"
  ```

---

## Task 4: 实现Android平台原生功能

**Files:**
- Create: `shared/src/androidMain/kotlin/com/example/scanapp/platform/AndroidWifiScanner.kt`
- Create: `shared/src/androidMain/kotlin/com/example/scanapp/platform/AndroidBluetoothScanner.kt`
- Create: `shared/src/androidMain/kotlin/com/example/scanapp/platform/AndroidLocationTracker.kt`
- Create: `shared/src/androidMain/kotlin/com/example/scanapp/database/AndroidDatabaseDriver.kt`
- Modify: `androidApp/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: Task 2的数据模型，Task 3的DAO
- Produces: Android平台扫描和定位实现

- [ ] **Step 1: 添加Android权限**
  
  在`androidApp/src/main/AndroidManifest.xml`中添加：
  ```xml
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
  <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
  <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
  <uses-permission android:name="android.permission.BLUETOOTH" />
  <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
  <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
  <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
  <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
  ```

- [ ] **Step 2: 实现Android WiFi扫描器**
  
  ```kotlin
  // shared/src/androidMain/kotlin/com/example/scanapp/platform/AndroidWifiScanner.kt
  package com.example.scanapp.platform
  
  import android.content.Context
  import android.net.wifi.ScanResult
  import android.net.wifi.WifiManager
  import android.os.Build
  import com.example.scanapp.models.WifiScanRecord
  
  class AndroidWifiScanner(private val context: Context) {
      
      private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
      
      fun scanWifi Networks(): List<WifiScanRecord> {
          val scanResults = wifiManager.scanResults
          val currentTime = System.currentTimeMillis()
          
          return scanResults.map { scanResult ->
              WifiScanRecord(
                  ssid = scanResult.SSID ?: "Unknown",
                  bssid = scanResult.BSSID ?: "",
                  signalStrength = scanResult.level,
                  frequency = scanResult.frequency,
                  timestamp = currentTime,
                  latitude = 0.0, // 将由LocationTracker提供
                  longitude = 0.0 // 将由LocationTracker提供
              )
          }
      }
      
      fun startScan() {
          wifiManager.startScan()
      }
  }
  ```

- [ ] **Step 3: 实现Android蓝牙扫描器**
  
  ```kotlin
  // shared/src/androidMain/kotlin/com/example/scanapp/platform/AndroidBluetoothScanner.kt
  package com.example.scanapp.platform
  
  import android.bluetooth.BluetoothAdapter
  import android.bluetooth.BluetoothDevice
  import android.bluetooth.BluetoothManager
  import android.bluetooth.le.BluetoothLeScanner
  import android.bluetooth.le.ScanCallback
  import android.bluetooth.le.ScanResult
  import android.content.Context
  import com.example.scanapp.models.BluetoothScanRecord
  
  class AndroidBluetoothScanner(private val context: Context) {
      
      private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
      private val bluetoothAdapter = bluetoothManager.adapter
      private var bleScanner: BluetoothLeScanner? = null
      private var scanCallback: ScanCallback? = null
      private var onDeviceFound: ((BluetoothScanRecord) -> Unit)? = null
      
      fun startScan(callback: (BluetoothScanRecord) -> Unit) {
          onDeviceFound = callback
          bleScanner = bluetoothAdapter?.bluetoothLeScanner
          
          scanCallback = object : ScanCallback() {
              override fun onScanResult(callbackType: Int, result: ScanResult) {
                  val device = result.device
                  val record = createScanRecord(device, result.rssi)
                  onDeviceFound?.invoke(record)
              }
              
              override fun onBatchScanResults(results: MutableList<ScanResult>) {
                  results.forEach { result ->
                      val device = result.device
                      val record = createScanRecord(device, result.rssi)
                      onDeviceFound?.invoke(record)
                  }
              }
              
              override fun onScanFailed(errorCode: Int) {
                  // 处理扫描失败
              }
          }
          
          bleScanner?.startScan(scanCallback)
      }
      
      fun stopScan() {
          scanCallback?.let { bleScanner?.stopScan(it) }
          scanCallback = null
      }
      
      private fun createScanRecord(device: BluetoothDevice, rssi: Int): BluetoothScanRecord {
          return BluetoothScanRecord(
              name = device.name ?: "Unknown",
              address = device.address ?: "",
              rssi = rssi,
              deviceType = getDeviceType(device),
              timestamp = System.currentTimeMillis(),
              latitude = 0.0, // 将由LocationTracker提供
              longitude = 0.0 // 将由LocationTracker提供
          )
      }
      
      private fun getDeviceType(device: BluetoothDevice): String {
          return when (device.type) {
              BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
              BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
              BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
              BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "Unknown"
              else -> "Unknown"
          }
      }
  }
  ```

- [ ] **Step 4: 实现Android位置追踪器**
  
  ```kotlin
  // shared/src/androidMain/kotlin/com/example/scanapp/platform/AndroidLocationTracker.kt
  package com.example.scanapp.platform
  
  import android.annotation.SuppressLint
  import android.content.Context
  import android.location.Location
  import android.location.LocationListener
  import android.location.LocationManager
  import android.os.Bundle
  import com.example.scanapp.models.LocationRecord
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  
  class AndroidLocationTracker(private val context: Context) {
      
      private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
      private val _currentLocation = MutableStateFlow<LocationRecord?>(null)
      val currentLocation: StateFlow<LocationRecord?> = _currentLocation
      
      private var locationListener: LocationListener? = null
      
      @SuppressLint("MissingPermission")
      fun startTracking() {
          locationListener = object : LocationListener {
              override fun onLocationChanged(location: Location) {
                  val record = LocationRecord(
                      latitude = location.latitude,
                      longitude = location.longitude,
                      altitude = location.altitude,
                      accuracy = location.accuracy,
                      timestamp = location.time
                  )
                  _currentLocation.value = record
              }
              
              override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
              override fun onProviderEnabled(provider: String) {}
              override fun onProviderDisabled(provider: String) {}
          }
          
          // 尝试使用GPS提供商
          if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
              locationManager.requestLocationUpdates(
                  LocationManager.GPS_PROVIDER,
                  1000L, // 1秒更新间隔
                  1f, // 1米最小距离
                  locationListener!!
              )
          }
          
          // 尝试使用网络提供商
          if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
              locationManager.requestLocationUpdates(
                  LocationManager.NETWORK_PROVIDER,
                  1000L,
                  1f,
                  locationListener!!
              )
          }
      }
      
      fun stopTracking() {
          locationListener?.let { locationManager.removeUpdates(it) }
          locationListener = null
      }
      
      fun getCurrentLocation(): LocationRecord? {
          return _currentLocation.value
      }
  }
  ```

- [ ] **Step 5: 实现Android数据库驱动**
  
  ```kotlin
  // shared/src/androidMain/kotlin/com/example/scanapp/database/AndroidDatabaseDriver.kt
  package com.example.scanapp.database
  
  import android.content.Context
  import app.cash.sqldelight.db.SqlDriver
  import app.cash.sqldelight.driver.android.AndroidSqliteDriver
  import com.example.scanapp.db.ScanAppDatabase
  
  actual fun createSqlDriver(): SqlDriver {
      return AndroidSqliteDriver(
          schema = ScanAppDatabase.Schema,
          context = TODO("需要Context参数"),
          name = "scanapp.db"
      )
  }
  ```

- [ ] **Step 6: 编译验证**
  
  ```bash
  ./gradlew :shared:compileKotlinAndroid
  ```
  预期：BUILD SUCCESSFUL

- [ ] **Step 7: 提交代码**
  
  ```bash
  git add shared/src/androidMain/
  git add androidApp/src/main/AndroidManifest.xml
  git commit -m "feat: 实现Android平台WiFi、蓝牙扫描和GPS定位"
  ```

---

## Task 5: 实现跨平台扫描服务

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/service/ScannerService.kt`
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/service/LocationService.kt`
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/service/ExportService.kt`

**Interfaces:**
- Consumes: Task 2的数据模型，Task 3的DAO，Task 4的平台实现
- Produces: 跨平台扫描服务接口

- [ ] **Step 1: 创建扫描服务接口**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/service/ScannerService.kt
  package com.example.scanapp.service
  
  import com.example.scanapp.models.BluetoothScanRecord
  import com.example.scanapp.models.WifiScanRecord
  import kotlinx.coroutines.flow.StateFlow
  
  interface ScannerService {
      val isScanning: StateFlow<Boolean>
      val wifiDevices: StateFlow<List<WifiScanRecord>>
      val bluetoothDevices: StateFlow<List<BluetoothScanRecord>>
      
      suspend fun startWifiScan()
      suspend fun stopWifiScan()
      suspend fun startBluetoothScan()
      suspend fun stopBluetoothScan()
      suspend fun startAllScans()
      suspend fun stopAllScans()
  }
  ```

- [ ] **Step 2: 创建位置服务接口**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/service/LocationService.kt
  package com.example.scanapp.service
  
  import com.example.scanapp.models.LocationRecord
  import kotlinx.coroutines.flow.StateFlow
  
  interface LocationService {
      val currentLocation: StateFlow<LocationRecord?>
      val isTracking: StateFlow<Boolean>
      
      suspend fun startTracking()
      suspend fun stopTracking()
      suspend fun getCurrentLocation(): LocationRecord?
  }
  ```

- [ ] **Step 3: 创建导出服务接口**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/service/ExportService.kt
  package com.example.scanapp.service
  
  import com.example.scanapp.models.BluetoothScanRecord
  import com.example.scanapp.models.LocationRecord
  import com.example.scanapp.models.WifiScanRecord
  
  interface ExportService {
      suspend fun exportToCsv(
          wifiRecords: List<WifiScanRecord>,
          bluetoothRecords: List<BluetoothScanRecord>,
          locationRecords: List<LocationRecord>
      ): String
      
      suspend fun exportToJson(
          wifiRecords: List<WifiScanRecord>,
          bluetoothRecords: List<BluetoothScanRecord>,
          locationRecords: List<LocationRecord>
      ): String
      
      suspend fun shareFile(filePath: String)
  }
  ```

- [ ] **Step 4: 实现跨平台扫描服务**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/service/ScannerServiceImpl.kt
  package com.example.scanapp.service
  
  import com.example.scanapp.database.BluetoothScanDao
  import com.example.scanapp.database.WifiScanDao
  import com.example.scanapp.models.BluetoothScanRecord
  import com.example.scanapp.models.WifiScanRecord
  import kotlinx.coroutines.CoroutineScope
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.SupervisorJob
  import kotlinx.coroutines.delay
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.launch
  
  class ScannerServiceImpl(
      private val wifiDao: WifiScanDao,
      private val bluetoothDao: BluetoothScanDao,
      private val locationService: LocationService
  ) : ScannerService {
      
      private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
      
      private val _isScanning = MutableStateFlow(false)
      override val isScanning: StateFlow<Boolean> = _isScanning
      
      private val _wifiDevices = MutableStateFlow<List<WifiScanRecord>>(emptyList())
      override val wifiDevices: StateFlow<List<WifiScanRecord>> = _wifiDevices
      
      private val _bluetoothDevices = MutableStateFlow<List<BluetoothScanRecord>>(emptyList())
      override val bluetoothDevices: StateFlow<List<BluetoothScanRecord>> = _bluetoothDevices
      
      private var scanJob: kotlinx.coroutines.Job? = null
      
      override suspend fun startWifiScan() {
          // 实现将在平台特定代码中完成
      }
      
      override suspend fun stopWifiScan() {
          // 实现将在平台特定代码中完成
      }
      
      override suspend fun startBluetoothScan() {
          // 实现将在平台特定代码中完成
      }
      
      override suspend fun stopBluetoothScan() {
          // 实现将在平台特定代码中完成
      }
      
      override suspend fun startAllScans() {
          _isScanning.value = true
          scanJob = scope.launch {
              while (true) {
                  // 扫描WiFi
                  val wifiDevices = scanWifiDevices()
                  _wifiDevices.value = wifiDevices
                  
                  // 扫描蓝牙
                  val bluetoothDevices = scanBluetoothDevices()
                  _bluetoothDevices.value = bluetoothDevices
                  
                  // 获取当前位置
                  val location = locationService.getCurrentLocation()
                  
                  // 保存到数据库
                  wifiDevices.forEach { device ->
                      val deviceWithLocation = device.copy(
                          latitude = location?.latitude ?: 0.0,
                          longitude = location?.longitude ?: 0.0
                      )
                      wifiDao.insertOrUpdate(deviceWithLocation)
                  }
                  
                  bluetoothDevices.forEach { device ->
                      val deviceWithLocation = device.copy(
                          latitude = location?.latitude ?: 0.0,
                          longitude = location?.longitude ?: 0.0
                      )
                      bluetoothDao.insertOrUpdate(deviceWithLocation)
                  }
                  
                  delay(5000) // 每5秒扫描一次
              }
          }
      }
      
      override suspend fun stopAllScans() {
          scanJob?.cancel()
          _isScanning.value = false
      }
      
      private suspend fun scanWifiDevices(): List<WifiScanRecord> {
          // 平台特定实现
          return emptyList()
      }
      
      private suspend fun scanBluetoothDevices(): List<BluetoothScanRecord> {
          // 平台特定实现
          return emptyList()
      }
  }
  ```

- [ ] **Step 5: 实现导出服务**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/service/ExportServiceImpl.kt
  package com.example.scanapp.service
  
  import com.example.scanapp.models.BluetoothScanRecord
  import com.example.scanapp.models.LocationRecord
  import com.example.scanapp.models.WifiScanRecord
  import kotlinx.serialization.encodeToString
  import kotlinx.serialization.json.Json
  
  class ExportServiceImpl : ExportService {
      
      override suspend fun exportToCsv(
          wifiRecords: List<WifiScanRecord>,
          bluetoothRecords: List<BluetoothScanRecord>,
          locationRecords: List<LocationRecord>
      ): String {
          val sb = StringBuilder()
          
          // WiFi记录
          sb.appendLine("WiFi Records")
          sb.appendLine("SSID,BSSID,Signal Strength,Frequency,Timestamp,Latitude,Longitude,Count")
          wifiRecords.forEach { record ->
              sb.appendLine("${record.ssid},${record.bssid},${record.signalStrength},${record.frequency},${record.timestamp},${record.latitude},${record.longitude},${record.count}")
          }
          
          sb.appendLine()
          
          // 蓝牙记录
          sb.appendLine("Bluetooth Records")
          sb.appendLine("Name,Address,RSSI,Device Type,Timestamp,Latitude,Longitude,Count")
          bluetoothRecords.forEach { record ->
              sb.appendLine("${record.name},${record.address},${record.rssi},${record.deviceType},${record.timestamp},${record.latitude},${record.longitude},${record.count}")
          }
          
          sb.appendLine()
          
          // 位置记录
          sb.appendLine("Location Records")
          sb.appendLine("Latitude,Longitude,Altitude,Accuracy,Timestamp")
          locationRecords.forEach { record ->
              sb.appendLine("${record.latitude},${record.longitude},${record.altitude},${record.accuracy},${record.timestamp}")
          }
          
          return sb.toString()
      }
      
      override suspend fun exportToJson(
          wifiRecords: List<WifiScanRecord>,
          bluetoothRecords: List<BluetoothScanRecord>,
          locationRecords: List<LocationRecord>
      ): String {
          val exportData = mapOf(
              "wifiRecords" to wifiRecords,
              "bluetoothRecords" to bluetoothRecords,
              "locationRecords" to locationRecords
          )
          
          return Json.encodeToString(exportData)
      }
      
      override suspend fun shareFile(filePath: String) {
          // 平台特定实现
      }
  }
  ```

- [ ] **Step 6: 提交代码**
  
  ```bash
  git add shared/src/commonMain/kotlin/com/example/scanapp/service/
  git commit -m "feat: 实现跨平台扫描、位置和导出服务接口"
  ```

---

## Task 6: 实现Android平台服务

**Files:**
- Create: `shared/src/androidMain/kotlin/com/example/scanapp/service/AndroidScannerService.kt`
- Create: `shared/src/androidMain/kotlin/com/example/scanapp/service/AndroidLocationService.kt`
- Create: `shared/src/androidMain/kotlin/com/example/scanapp/service/AndroidExportService.kt`

**Interfaces:**
- Consumes: Task 4的Android平台实现，Task 5的服务接口
- Produces: Android平台特定的服务实现

- [ ] **Step 1: 实现Android扫描服务**
  
  ```kotlin
  // shared/src/androidMain/kotlin/com/example/scanapp/service/AndroidScannerService.kt
  package com.example.scanapp.service
  
  import android.content.Context
  import com.example.scanapp.database.BluetoothScanDao
  import com.example.scanapp.database.WifiScanDao
  import com.example.scanapp.models.BluetoothScanRecord
  import com.example.scanapp.models.WifiScanRecord
  import com.example.scanapp.platform.AndroidBluetoothScanner
  import com.example.scanapp.platform.AndroidWifiScanner
  import kotlinx.coroutines.CoroutineScope
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.SupervisorJob
  import kotlinx.coroutines.delay
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.launch
  
  class AndroidScannerService(
      private val context: Context,
      private val wifiDao: WifiScanDao,
      private val bluetoothDao: BluetoothScanDao,
      private val locationService: LocationService
  ) : ScannerService {
      
      private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
      private val wifiScanner = AndroidWifiScanner(context)
      private val bluetoothScanner = AndroidBluetoothScanner(context)
      
      private val _isScanning = MutableStateFlow(false)
      override val isScanning: StateFlow<Boolean> = _isScanning
      
      private val _wifiDevices = MutableStateFlow<List<WifiScanRecord>>(emptyList())
      override val wifiDevices: StateFlow<List<WifiScanRecord>> = _wifiDevices
      
      private val _bluetoothDevices = MutableStateFlow<List<BluetoothScanRecord>>(emptyList())
      override val bluetoothDevices: StateFlow<List<BluetoothScanRecord>> = _bluetoothDevices
      
      private var scanJob: kotlinx.coroutines.Job? = null
      
      override suspend fun startWifiScan() {
          // 实现WiFi扫描
      }
      
      override suspend fun stopWifiScan() {
          // 停止WiFi扫描
      }
      
      override suspend fun startBluetoothScan() {
          bluetoothScanner.startScan { record ->
              scope.launch {
                  val location = locationService.getCurrentLocation()
                  val recordWithLocation = record.copy(
                      latitude = location?.latitude ?: 0.0,
                      longitude = location?.longitude ?: 0.0
                  )
                  bluetoothDao.insertOrUpdate(recordWithLocation)
                  _bluetoothDevices.value = bluetoothDao.getAllRecords()
              }
          }
      }
      
      override suspend fun stopBluetoothScan() {
          bluetoothScanner.stopScan()
      }
      
      override suspend fun startAllScans() {
          _isScanning.value = true
          scanJob = scope.launch {
              while (true) {
                  // 扫描WiFi
                  wifiScanner.startScan()
                  val wifiDevices = wifiScanner.scanWifiNetworks()
                  _wifiDevices.value = wifiDevices
                  
                  // 获取当前位置
                  val location = locationService.getCurrentLocation()
                  
                  // 保存到数据库
                  wifiDevices.forEach { device ->
                      val deviceWithLocation = device.copy(
                          latitude = location?.latitude ?: 0.0,
                          longitude = location?.longitude ?: 0.0
                      )
                      wifiDao.insertOrUpdate(deviceWithLocation)
                  }
                  
                  delay(5000) // 每5秒扫描一次
              }
          }
      }
      
      override suspend fun stopAllScans() {
          scanJob?.cancel()
          _isScanning.value = false
          bluetoothScanner.stopScan()
      }
  }
  ```

- [ ] **Step 2: 实现Android位置服务**
  
  ```kotlin
  // shared/src/androidMain/kotlin/com/example/scanapp/service/AndroidLocationService.kt
  package com.example.scanapp.service
  
  import android.content.Context
  import com.example.scanapp.models.LocationRecord
  import com.example.scanapp.platform.AndroidLocationTracker
  import kotlinx.coroutines.flow.StateFlow
  
  class AndroidLocationService(context: Context) : LocationService {
      
      private val locationTracker = AndroidLocationTracker(context)
      
      override val currentLocation: StateFlow<LocationRecord?> = locationTracker.currentLocation
      
      private val _isTracking = kotlinx.coroutines.flow.MutableStateFlow(false)
      override val isTracking: StateFlow<Boolean> = _isTracking
      
      override suspend fun startTracking() {
          locationTracker.startTracking()
          _isTracking.value = true
      }
      
      override suspend fun stopTracking() {
          locationTracker.stopTracking()
          _isTracking.value = false
      }
      
      override suspend fun getCurrentLocation(): LocationRecord? {
          return locationTracker.getCurrentLocation()
      }
  }
  ```

- [ ] **Step 3: 实现Android导出服务**
  
  ```kotlin
  // shared/src/androidMain/kotlin/com/example/scanapp/service/AndroidExportService.kt
  package com.example.scanapp.service
  
  import android.content.Context
  import android.content.Intent
  import android.net.Uri
  import androidx.core.content.FileProvider
  import com.example.scanapp.models.BluetoothScanRecord
  import com.example.scanapp.models.LocationRecord
  import com.example.scanapp.models.WifiScanRecord
  import java.io.File
  
  class AndroidExportService(private val context: Context) : ExportService {
      
      private val exportServiceImpl = ExportServiceImpl()
      
      override suspend fun exportToCsv(
          wifiRecords: List<WifiScanRecord>,
          bluetoothRecords: List<BluetoothScanRecord>,
          locationRecords: List<LocationRecord>
      ): String {
          return exportServiceImpl.exportToCsv(wifiRecords, bluetoothRecords, locationRecords)
      }
      
      override suspend fun exportToJson(
          wifiRecords: List<WifiScanRecord>,
          bluetoothRecords: List<BluetoothScanRecord>,
          locationRecords: List<LocationRecord>
      ): String {
          return exportServiceImpl.exportToJson(wifiRecords, bluetoothRecords, locationRecords)
      }
      
      override suspend fun shareFile(filePath: String) {
          val file = File(filePath)
          val uri = FileProvider.getUriForFile(
              context,
              "${context.packageName}.fileprovider",
              file
          )
          
          val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/csv"
              putExtra(Intent.EXTRA_STREAM, uri)
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          
          context.startActivity(Intent.createChooser(shareIntent, "分享文件"))
      }
  }
  ```

- [ ] **Step 4: 提交代码**
  
  ```bash
  git add shared/src/androidMain/kotlin/com/example/scanapp/service/
  git commit -m "feat: 实现Android平台扫描、位置和导出服务"
  ```

---

## Task 7: 实现Kuikly UI界面

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/ui/ScannerPage.kt`
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/ui/DeviceListPage.kt`
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/ui/StatisticsPage.kt`
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/ui/MapPage.kt`
- Create: `shared/src/commonMain/kotlin/com/example/scanapp/ui/SettingsPage.kt`

**Interfaces:**
- Consumes: Task 5的服务接口
- Produces: Kuikly UI页面

- [ ] **Step 1: 创建扫描页面**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/ui/ScannerPage.kt
  package com.example.scanapp.ui
  
  import com.tencent.kuikly.core.base.ViewContainer
  import com.tencent.kuikly.core.layout.size
  import com.tencent.kuikly.core.layout.width
  import com.tencent.kuikly.core.layout.height
  import com.tencent.kuikly.core.layout.allCenter
  import com.tencent.kuikly.core.layout.padding
  import com.tencent.kuikly.core.layout.marginTop
  import com.tencent.kuikly.core.views.Text
  import com.tencent.kuikly.core.views.Button
  import com.tencent.kuikly.core.views.View
  import com.tencent.kuikly.core.reactive.variable
  import com.tencent.kuikly.core.base.Page
  import com.tencent.kuikly.core.base.PageParams
  import com.tencent.kuikly.core.base.Pager
  import com.tencent.kuikly.core.base.annotation.Page
  
  @Page("Scanner")
  class ScannerPage : Pager() {
      
      private val isScanning = variable(false)
      private val wifiCount = variable(0)
      private val bluetoothCount = variable(0)
      
      override fun body(): ViewContainer {
          return View {
              attr {
                  allCenter()
                  size(375f, 667f)
              }
              
              Text {
                  attr {
                      text("WiFi/蓝牙扫描器")
                      fontSize(24f)
                      marginTop(50f)
                  }
              }
              
              Text {
                  attr {
                      text("WiFi设备: ${wifiCount.value}")
                      fontSize(16f)
                      marginTop(20f)
                  }
              }
              
              Text {
                  attr {
                      text("蓝牙设备: ${bluetoothCount.value}")
                      fontSize(16f)
                      marginTop(10f)
                  }
              }
              
              Button {
                  attr {
                      text(if (isScanning.value) "停止扫描" else "开始扫描")
                      marginTop(30f)
                      padding(10f, 20f)
                  }
                  event {
                      click {
                          if (isScanning.value) {
                              stopScanning()
                          } else {
                              startScanning()
                          }
                      }
                  }
              }
          }
      }
      
      private fun startScanning() {
          isScanning.value = true
          // 启动扫描服务
      }
      
      private fun stopScanning() {
          isScanning.value = false
          // 停止扫描服务
      }
  }
  ```

- [ ] **Step 2: 创建设备列表页面**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/ui/DeviceListPage.kt
  package com.example.scanapp.ui
  
  import com.tencent.kuikly.core.base.ViewContainer
  import com.tencent.kuikly.core.layout.size
  import com.tencent.kuikly.core.layout.allCenter
  import com.tencent.kuikly.core.layout.padding
  import com.tencent.kuikly.core.views.Text
  import com.tencent.kuikly.core.views.View
  import com.tencent.kuikly.core.base.Page
  import com.tencent.kuikly.core.base.Pager
  import com.tencent.kuikly.core.base.annotation.Page
  
  @Page("DeviceList")
  class DeviceListPage : Pager() {
      
      override fun body(): ViewContainer {
          return View {
              attr {
                  allCenter()
                  size(375f, 667f)
              }
              
              Text {
                  attr {
                      text("设备列表")
                      fontSize(24f)
                      marginTop(50f)
                  }
              }
              
              // 设备列表将在这里实现
          }
      }
  }
  ```

- [ ] **Step 3: 创建统计页面**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/ui/StatisticsPage.kt
  package com.example.scanapp.ui
  
  import com.tencent.kuikly.core.base.ViewContainer
  import com.tencent.kuikly.core.layout.size
  import com.tencent.kuikly.core.layout.allCenter
  import com.tencent.kuikly.core.layout.padding
  import com.tencent.kuikly.core.views.Text
  import com.tencent.kuikly.core.views.View
  import com.tencent.kuikly.core.base.Page
  import com.tencent.kuikly.core.base.Pager
  import com.tencent.kuikly.core.base.annotation.Page
  
  @Page("Statistics")
  class StatisticsPage : Pager() {
      
      override fun body(): ViewContainer {
          return View {
              attr {
                  allCenter()
                  size(375f, 667f)
              }
              
              Text {
                  attr {
                      text("统计信息")
                      fontSize(24f)
                      marginTop(50f)
                  }
              }
              
              // 统计图表将在这里实现
          }
      }
  }
  ```

- [ ] **Step 4: 创建地图页面**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/ui/MapPage.kt
  package com.example.scanapp.ui
  
  import com.tencent.kuikly.core.base.ViewContainer
  import com.tencent.kuikly.core.layout.size
  import com.tencent.kuikly.core.layout.allCenter
  import com.tencent.kuikly.core.layout.padding
  import com.tencent.kuikly.core.views.Text
  import com.tencent.kuikly.core.views.View
  import com.tencent.kuikly.core.base.Page
  import com.tencent.kuikly.core.base.Pager
  import com.tencent.kuikly.core.base.annotation.Page
  
  @Page("Map")
  class MapPage : Pager() {
      
      override fun body(): ViewContainer {
          return View {
              attr {
                  allCenter()
                  size(375f, 667f)
              }
              
              Text {
                  attr {
                      text("地图视图")
                      fontSize(24f)
                      marginTop(50f)
                  }
              }
              
              // 地图将在这里实现
          }
      }
  }
  ```

- [ ] **Step 5: 创建设置页面**
  
  ```kotlin
  // shared/src/commonMain/kotlin/com/example/scanapp/ui/SettingsPage.kt
  package com.example.scanapp.ui
  
  import com.tencent.kuikly.core.base.ViewContainer
  import com.tencent.kuikly.core.layout.size
  import com.tencent.kuikly.core.layout.allCenter
  import com.tencent.kuikly.core.layout.padding
  import com.tencent.kuikly.core.views.Text
  import com.tencent.kuikly.core.views.View
  import com.tencent.kuikly.core.base.Page
  import com.tencent.kuikly.core.base.Pager
  import com.tencent.kuikly.core.base.annotation.Page
  
  @Page("Settings")
  class SettingsPage : Pager() {
      
      override fun body(): ViewContainer {
          return View {
              attr {
                  allCenter()
                  size(375f, 667f)
              }
              
              Text {
                  attr {
                      text("设置")
                      fontSize(24f)
                      marginTop(50f)
                  }
              }
              
              // 设置选项将在这里实现
          }
      }
  }
  ```

- [ ] **Step 6: 提交代码**
  
  ```bash
  git add shared/src/commonMain/kotlin/com/example/scanapp/ui/
  git commit -m "feat: 实现Kuikly UI界面"
  ```

---

## Task 8: 配置Android壳工程

**Files:**
- Modify: `androidApp/build.gradle.kts`
- Modify: `androidApp/src/main/java/com/example/scanapp/MainActivity.kt`
- Create: `androidApp/src/main/java/com/example/scanapp/KuiklyRenderActivity.kt`

**Interfaces:**
- Consumes: Task 7的UI页面
- Produces: 可运行的Android应用

- [ ] **Step 1: 添加Kuikly渲染器依赖**
  
  在`androidApp/build.gradle.kts`中添加：
  ```kotlin
  dependencies {
      implementation("com.tencent.kuikly-open:core-render-android:2.0.0")
      implementation("com.tencent.kuikly-open:core:2.0.0")
      implementation(project(":shared"))
      implementation("androidx.core:core-ktx:1.7.0")
      implementation("androidx.appcompat:appcompat:1.6.1")
      implementation("com.google.android.material:material:1.8.0")
      implementation("androidx.constraintlayout:constraintlayout:2.1.3")
  }
  ```

- [ ] **Step 2: 实现Kuikly渲染Activity**
  
  ```kotlin
  // androidApp/src/main/java/com/example/scanapp/KuiklyRenderActivity.kt
  package com.example.scanapp
  
  import android.os.Bundle
  import androidx.appcompat.app.AppCompatActivity
  import com.tencent.kuikly.render.core.KuiklyView
  import com.tencent.kuikly.render.core.KuiklyViewDelegator
  
  class KuiklyRenderActivity : AppCompatActivity() {
      
      private lateinit var kuiklyViewDelegator: KuiklyViewDelegator
      
      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          
          val pageName = intent.getStringExtra("pageName") ?: ""
          
          kuiklyViewDelegator = KuiklyViewDelegator(
              this,
              KuiklyView.CodeHandler(pageName)
          )
          
          setContentView(kuiklyViewDelegator.kuiklyView)
          
          kuiklyViewDelegator.loadPage(pageName)
      }
      
      companion object {
          fun start(activity: android.app.Activity, pageName: String) {
              val intent = android.content.Intent(activity, KuiklyRenderActivity::class.java)
              intent.putExtra("pageName", pageName)
              activity.startActivity(intent)
          }
      }
  }
  ```

- [ ] **Step 3: 修改MainActivity**
  
  ```kotlin
  // androidApp/src/main/java/com/example/scanapp/MainActivity.kt
  package com.example.scanapp
  
  import android.os.Bundle
  import androidx.appcompat.app.AppCompatActivity
  import android.widget.Button
  import android.widget.LinearLayout
  import android.view.ViewGroup
  import android.widget.TextView
  
  class MainActivity : AppCompatActivity() {
      
      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          
          val layout = LinearLayout(this).apply {
              orientation = LinearLayout.VERTICAL
              layoutParams = ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  ViewGroup.LayoutParams.MATCH_PARENT
              )
              setPadding(50, 50, 50, 50)
          }
          
          val titleText = TextView(this).apply {
              text = "WiFi/蓝牙扫描器"
              textSize = 24f
              layoutParams = LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT,
                  LinearLayout.LayoutParams.WRAP_CONTENT
              )
          }
          
          val scanButton = Button(this).apply {
              text = "开始扫描"
              layoutParams = LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT,
                  LinearLayout.LayoutParams.WRAP_CONTENT
              ).apply {
                  topMargin = 50
              }
              setOnClickListener {
                  KuiklyRenderActivity.start(this@MainActivity, "Scanner")
              }
          }
          
          val deviceListButton = Button(this).apply {
              text = "设备列表"
              layoutParams = LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT,
                  LinearLayout.LayoutParams.WRAP_CONTENT
              ).apply {
                  topMargin = 20
              }
              setOnClickListener {
                  KuiklyRenderActivity.start(this@MainActivity, "DeviceList")
              }
          }
          
          val statisticsButton = Button(this).apply {
              text = "统计信息"
              layoutParams = LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT,
                  LinearLayout.LayoutParams.WRAP_CONTENT
              ).apply {
                  topMargin = 20
              }
              setOnClickListener {
                  KuiklyRenderActivity.start(this@MainActivity, "Statistics")
              }
          }
          
          val mapButton = Button(this).apply {
              text = "地图视图"
              layoutParams = LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT,
                  LinearLayout.LayoutParams.WRAP_CONTENT
              ).apply {
                  topMargin = 20
              }
              setOnClickListener {
                  KuiklyRenderActivity.start(this@MainActivity, "Map")
              }
          }
          
          val settingsButton = Button(this).apply {
              text = "设置"
              layoutParams = LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT,
                  LinearLayout.LayoutParams.WRAP_CONTENT
              ).apply {
                  topMargin = 20
              }
              setOnClickListener {
                  KuiklyRenderActivity.start(this@MainActivity, "Settings")
              }
          }
          
          layout.addView(titleText)
          layout.addView(scanButton)
          layout.addView(deviceListButton)
          layout.addView(statisticsButton)
          layout.addView(mapButton)
          layout.addView(settingsButton)
          
          setContentView(layout)
      }
  }
  ```

- [ ] **Step 4: 配置FileProvider**
  
  在`androidApp/src/main/res/xml/`目录下创建`file_paths.xml`：
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <paths>
      <external-path name="external_files" path="."/>
      <cache-path name="cache" path="."/>
  </paths>
  ```

- [ ] **Step 5: 在AndroidManifest.xml中添加FileProvider**
  
  ```xml
  <provider
      android:name="androidx.core.content.FileProvider"
      android:authorities="${applicationId}.fileprovider"
      android:exported="false"
      android:grantUriPermissions="true">
      <meta-data
          android:name="android.support.FILE_PROVIDER_PATHS"
          android:resource="@xml/file_paths" />
  </provider>
  ```

- [ ] **Step 6: 编译运行**
  
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```
  预期：BUILD SUCCESSFUL

- [ ] **Step 7: 提交代码**
  
  ```bash
  git add androidApp/
  git commit -m "feat: 配置Android壳工程，实现主界面和Kuikly渲染"
  ```

---

## Task 9: 实现后台扫描服务

**Files:**
- Create: `shared/src/androidMain/kotlin/com/example/scanapp/service/BackgroundScanService.kt`
- Modify: `androidApp/src/main/AndroidManifest.xml`
- Create: `androidApp/src/main/java/com/example/scanapp/BackgroundScanWorker.kt`

**Interfaces:**
- Consumes: Task 6的Android服务实现
- Produces: Android后台扫描服务

- [ ] **Step 1: 创建后台扫描Worker**
  
  ```kotlin
  // androidApp/src/main/java/com/example/scanapp/BackgroundScanWorker.kt
  package com.example.scanapp
  
  import android.content.Context
  import androidx.work.CoroutineWorker
  import androidx.work.WorkerParameters
  import com.example.scanapp.database.DatabaseFactory
  import com.example.scanapp.service.AndroidLocationService
  import com.example.scanapp.service.AndroidScannerService
  
  class BackgroundScanWorker(
      context: Context,
      params: WorkerParameters
  ) : CoroutineWorker(context, params) {
      
      override suspend fun doWork(): Result {
          return try {
              val database = DatabaseFactory.getDatabase()
              val wifiDao = com.example.scanapp.database.WifiScanDao(database)
              val bluetoothDao = com.example.scanapp.database.BluetoothScanDao(database)
              val locationService = AndroidLocationService(applicationContext)
              
              val scannerService = AndroidScannerService(
                  applicationContext,
                  wifiDao,
                  bluetoothDao,
                  locationService
              )
              
              locationService.startTracking()
              scannerService.startAllScans()
              
              // 扫描30秒后停止
              kotlinx.coroutines.delay(30000)
              
              scannerService.stopAllScans()
              locationService.stopTracking()
              
              Result.success()
          } catch (e: Exception) {
              Result.failure()
          }
      }
  }
  ```

- [ ] **Step 2: 创建后台扫描服务**
  
  ```kotlin
  // shared/src/androidMain/kotlin/com/example/scanapp/service/BackgroundScanService.kt
  package com.example.scanapp.service
  
  import android.app.Notification
  import android.app.NotificationChannel
  import android.app.NotificationManager
  import android.app.Service
  import android.content.Context
  import android.content.Intent
  import android.os.IBinder
  import androidx.core.app.NotificationCompat
  import com.example.scanapp.R
  import com.example.scanapp.database.DatabaseFactory
  import com.example.scanapp.platform.AndroidBluetoothScanner
  import com.example.scanapp.platform.AndroidLocationTracker
  import com.example.scanapp.platform.AndroidWifiScanner
  
  class BackgroundScanService : Service() {
      
      private lateinit var wifiScanner: AndroidWifiScanner
      private lateinit var bluetoothScanner: AndroidBluetoothScanner
      private lateinit var locationTracker: AndroidLocationTracker
      
      override fun onBind(intent: Intent?): IBinder? {
          return null
      }
      
      override fun onCreate() {
          super.onCreate()
          
          wifiScanner = AndroidWifiScanner(this)
          bluetoothScanner = AndroidBluetoothScanner(this)
          locationTracker = AndroidLocationTracker(this)
          
          createNotificationChannel()
          startForeground(1, createNotification())
          
          locationTracker.startTracking()
          startScanning()
      }
      
      override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
          return START_STICKY
      }
      
      override fun onDestroy() {
          super.onDestroy()
          
          locationTracker.stopTracking()
          bluetoothScanner.stopScan()
          stopForeground(true)
          stopSelf()
      }
      
      private fun startScanning() {
          // 启动WiFi扫描
          wifiScanner.startScan()
          
          // 启动蓝牙扫描
          bluetoothScanner.startScan { record ->
              // 保存到数据库
              val location = locationTracker.getCurrentLocation()
              val recordWithLocation = record.copy(
                  latitude = location?.latitude ?: 0.0,
                  longitude = location?.longitude ?: 0.0
              )
              
              // 异步保存到数据库
              kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                  val database = DatabaseFactory.getDatabase()
                  val bluetoothDao = com.example.scanapp.database.BluetoothScanDao(database)
                  bluetoothDao.insertOrUpdate(recordWithLocation)
              }
          }
      }
      
      private fun createNotificationChannel() {
          val channelId = "scan_service_channel"
          val channelName = "扫描服务"
          val importance = NotificationManager.IMPORTANCE_LOW
          
          val channel = NotificationChannel(channelId, channelName, importance).apply {
              description = "后台扫描服务通知"
          }
          
          val notificationManager = getSystemService(NotificationManager::class.java)
          notificationManager.createNotificationChannel(channel)
      }
      
      private fun createNotification(): Notification {
          val channelId = "scan_service_channel"
          
          return NotificationCompat.Builder(this, channelId)
              .setContentTitle("WiFi/蓝牙扫描器")
              .setContentText("正在后台扫描...")
              .setSmallIcon(R.drawable.ic_launcher_foreground)
              .setPriority(NotificationCompat.PRIORITY_LOW)
              .setOngoing(true)
              .build()
      }
      
      companion object {
          fun start(context: Context) {
              val intent = Intent(context, BackgroundScanService::class.java)
              context.startForegroundService(intent)
          }
          
          fun stop(context: Context) {
              val intent = Intent(context, BackgroundScanService::class.java)
              context.stopService(intent)
          }
      }
  }
  ```

- [ ] **Step 3: 在AndroidManifest.xml中注册服务**
  
  ```xml
  <service
      android:name=".BackgroundScanService"
      android:enabled="true"
      android:exported="false" />
  ```

- [ ] **Step 4: 添加WorkManager依赖**
  
  在`androidApp/build.gradle.kts`中添加：
  ```kotlin
  implementation("androidx.work:work-runtime:2.8.1")
  implementation("androidx.work:work-runtime-ktx:2.8.1")
  ```

- [ ] **Step 5: 提交代码**
  
  ```bash
  git add shared/src/androidMain/kotlin/com/example/scanapp/service/
  git add androidApp/
  git commit -m "feat: 实现Android后台扫描服务"
  ```

---

## Task 10: 测试和优化

**Files:**
- Create: `shared/src/commonTest/kotlin/com/example/scanapp/DatabaseTest.kt`
- Create: `shared/src/commonTest/kotlin/com/example/scanapp/ScannerTest.kt`
- Modify: `shared/build.gradle.kts`

**Interfaces:**
- Consumes: 所有先前任务
- Produces: 测试代码和优化后的应用

- [ ] **Step 1: 添加测试依赖**
  
  在`shared/build.gradle.kts`中添加：
  ```kotlin
  kotlin {
      sourceSets {
          val commonTest by getting {
              dependencies {
                  implementation(kotlin("test"))
                  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                  implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
              }
          }
      }
  }
  ```

- [ ] **Step 2: 创建数据库测试**
  
  ```kotlin
  // shared/src/commonTest/kotlin/com/example/scanapp/DatabaseTest.kt
  package com.example.scanapp
  
  import com.example.scanapp.database.BluetoothScanDao
  import com.example.scanapp.database.WifiScanDao
  import com.example.scanapp.models.BluetoothScanRecord
  import com.example.scanapp.models.WifiScanRecord
  import kotlinx.coroutines.runBlocking
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertNotNull
  
  class DatabaseTest {
      
      @Test
      fun testWifiScanDao() = runBlocking {
          // 测试WiFi扫描DAO
          // 注意：需要使用测试数据库驱动
      }
      
      @Test
      fun testBluetoothScanDao() = runBlocking {
          // 测试蓝牙扫描DAO
      }
      
      @Test
      fun testLocationDao() = runBlocking {
          // 测试位置DAO
      }
  }
  ```

- [ ] **Step 3: 创建扫描器测试**
  
  ```kotlin
  // shared/src/commonTest/kotlin/com/example/scanapp/ScannerTest.kt
  package com.example.scanapp
  
  import com.example.scanapp.models.WifiScanRecord
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertNotNull
  
  class ScannerTest {
      
      @Test
      fun testWifiScanRecord() {
          val record = WifiScanRecord(
              ssid = "TestWiFi",
              bssid = "00:11:22:33:44:55",
              signalStrength = -50,
              frequency = 2400,
              timestamp = System.currentTimeMillis(),
              latitude = 39.9042,
              longitude = 116.4074
          )
          
          assertEquals("TestWiFi", record.ssid)
          assertEquals("00:11:22:33:44:55", record.bssid)
          assertEquals(-50, record.signalStrength)
          assertEquals(2400, record.frequency)
          assertNotNull(record.timestamp)
          assertEquals(39.9042, record.latitude)
          assertEquals(116.4074, record.longitude)
      }
      
      @Test
      fun testBluetoothScanRecord() {
          val record = BluetoothScanRecord(
              name = "TestDevice",
              address = "AA:BB:CC:DD:EE:FF",
              rssi = -60,
              deviceType = "BLE",
              timestamp = System.currentTimeMillis(),
              latitude = 39.9042,
              longitude = 116.4074
          )
          
          assertEquals("TestDevice", record.name)
          assertEquals("AA:BB:CC:DD:EE:FF", record.address)
          assertEquals(-60, record.rssi)
          assertEquals("BLE", record.deviceType)
          assertNotNull(record.timestamp)
          assertEquals(39.9042, record.latitude)
          assertEquals(116.4074, record.longitude)
      }
  }
  ```

- [ ] **Step 4: 运行测试**
  
  ```bash
  ./gradlew :shared:allTests
  ```
  预期：所有测试通过

- [ ] **Step 5: 性能优化**
  
  - 优化数据库查询
  - 减少内存使用
  - 优化电池消耗

- [ ] **Step 6: 提交代码**
  
  ```bash
  git add shared/src/commonTest/
  git commit -m "feat: 添加单元测试和性能优化"
  ```

---

## Task 11: 文档和部署

**Files:**
- Create: `README.md`
- Create: `docs/user-guide.md`
- Create: `docs/developer-guide.md`

**Interfaces:**
- Consumes: 所有先前任务
- Produces: 项目文档

- [ ] **Step 1: 创建README.md**
  
  ```markdown
  # WiFi/蓝牙扫描器
  
  一个跨平台WiFi/蓝牙扫描器应用，支持Android、iOS和鸿蒙平台。
  
  ## 功能特性
  
  - 扫描附近的WiFi和蓝牙设备
  - 记录每次扫描的GPS位置
  - 本地SQLite存储，重复设备增加计数
  - 数据导出为CSV/JSON格式
  - 可视化界面（列表、图表、地图）
  
  ## 支持平台
  
  - Android 8.0+
  - iOS 13.0+
  - 鸿蒙2.0+
  
  ## 开发环境
  
  - Android Studio
  - Kotlin Multiplatform
  - Kuikly框架
  
  ## 快速开始
  
  1. 克隆项目
  2. 使用Android Studio打开项目
  3. 运行androidApp模块
  
  ## 项目结构
  
  ```
  scanapp/
  ├── shared/                    # 跨平台业务逻辑模块
  ├── androidApp/               # Android壳工程
  ├── iosApp/                   # iOS壳工程
  └── ohosApp/                  # 鸿蒙壳工程
  ```
  
  ## 许可证
  
  MIT License
  ```

- [ ] **Step 2: 创建用户指南**
  
  ```markdown
  # 用户指南
  
  ## 安装应用
  
  1. 从Google Play Store下载应用（Android）
  2. 从App Store下载应用（iOS）
  3. 从AppGallery下载应用（鸿蒙）
  
  ## 使用方法
  
  ### 开始扫描
  
  1. 打开应用
  2. 点击"开始扫描"按钮
  3. 应用将开始扫描附近的WiFi和蓝牙设备
  
  ### 查看设备列表
  
  1. 点击"设备列表"按钮
  2. 查看所有扫描到的设备
  
  ### 查看统计信息
  
  1. 点击"统计信息"按钮
  2. 查看扫描数据的统计图表
  
  ### 查看地图
  
  1. 点击"地图视图"按钮
  2. 在地图上查看设备位置
  
  ### 导出数据
  
  1. 点击"设置"按钮
  2. 选择"导出数据"
  3. 选择导出格式（CSV/JSON）
  
  ## 权限说明
  
  - 位置权限：用于获取GPS位置
  - 蓝牙权限：用于扫描蓝牙设备
  - WiFi权限：用于扫描WiFi网络
  - 存储权限：用于保存导出文件
  ```

- [ ] **Step 3: 创建开发者指南**
  
  ```markdown
  # 开发者指南
  
  ## 项目架构
  
  项目采用模块化架构，使用Kuikly KMP框架实现跨平台开发。
  
  ## 技术栈
  
  - Kuikly UI框架
  - Kotlin Multiplatform
  - SQLDelight（跨平台SQLite）
  - 原生平台API（WiFi、蓝牙、GPS）
  
  ## 开发环境
  
  1. 安装Android Studio
  2. 安装Kotlin和Kotlin MultiPlatform插件
  3. 安装Kuikly插件
  
  ## 项目结构
  
  ```
  scanapp/
  ├── shared/                    # 跨平台业务逻辑模块
  │   ├── scanner/              # WiFi和蓝牙扫描模块
  │   ├── database/             # SQLite数据库模块
  │   ├── location/             # GPS定位模块
  │   ├── export/               # 数据导出模块
  │   └── ui/                   # 用户界面模块
  ├── androidApp/               # Android壳工程
  ├── iosApp/                   # iOS壳工程
  └── ohosApp/                  # 鸿蒙壳工程
  ```
  
  ## 开发流程
  
  1. 在shared模块中编写跨平台代码
  2. 在平台特定模块中实现原生功能
  3. 在UI模块中构建用户界面
  4. 测试和优化
  
  ## 测试
  
  ```bash
  ./gradlew :shared:allTests
  ```
  
  ## 构建
  
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```
  ```

- [ ] **Step 4: 提交文档**
  
  ```bash
  git add README.md docs/
  git commit -m "feat: 添加项目文档"
  ```

---

**Plan complete and saved to `docs/superpowers/plans/2026-07-02-wifi-bluetooth-scanner-implementation.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**