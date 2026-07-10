# Task 4: 实现Android平台原生功能

## 任务描述

实现Android平台的原生功能，包括：
- 添加Android权限
- 实现Android WiFi扫描器
- 实现Android蓝牙扫描器
- 实现Android位置追踪器
- 实现Android数据库驱动

## 具体步骤

### Step 1: 添加Android权限

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

### Step 2: 实现Android WiFi扫描器

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
    
    fun scanWifiNetworks(): List<WifiScanRecord> {
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

### Step 3: 实现Android蓝牙扫描器

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

### Step 4: 实现Android位置追踪器

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

### Step 5: 实现Android数据库驱动

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

### Step 6: 提交代码

```bash
git add shared/src/androidMain/
git add androidApp/src/main/AndroidManifest.xml
git commit -m "feat: 实现Android平台WiFi、蓝牙扫描和GPS定位"
```

## 接口

- Consumes: Task 2的数据模型，Task 3的DAO
- Produces: Android平台扫描和定位实现

## 全局约束

- 目标平台：Android 8.0+、iOS 13.0+、鸿蒙2.0+
- 使用Kuikly KMP工程模板
- 数据库使用SQLDelight
- UI使用Kuikly DSL构建
- 后台扫描需要平台特定的后台任务管理