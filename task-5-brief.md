# Task 5: 实现跨平台扫描服务

## 任务描述

实现跨平台扫描服务接口，包括：
- 扫描服务接口
- 位置服务接口
- 导出服务接口
- 跨平台扫描服务实现
- 导出服务实现

## 具体步骤

### Step 1: 创建扫描服务接口

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

### Step 2: 创建位置服务接口

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

### Step 3: 创建导出服务接口

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

### Step 4: 实现跨平台扫描服务

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

### Step 5: 实现导出服务

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

### Step 6: 提交代码

```bash
git add shared/src/commonMain/kotlin/com/example/scanapp/service/
git commit -m "feat: 实现跨平台扫描、位置和导出服务接口"
```

## 接口

- Consumes: Task 2的数据模型，Task 3的DAO，Task 4的平台实现
- Produces: 跨平台扫描服务接口

## 全局约束

- 目标平台：Android 8.0+、iOS 13.0+、鸿蒙2.0+
- 使用Kuikly KMP工程模板
- 数据库使用SQLDelight
- UI使用Kuikly DSL构建
- 后台扫描需要平台特定的后台任务管理