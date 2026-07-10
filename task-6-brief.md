# Task 6: 实现Android平台服务

## 任务描述

实现Android平台特定的服务，包括：
- Android扫描服务
- Android位置服务
- Android导出服务

## 具体步骤

### Step 1: 实现Android扫描服务

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

### Step 2: 实现Android位置服务

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

### Step 3: 实现Android导出服务

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

### Step 4: 提交代码

```bash
git add shared/src/androidMain/kotlin/com/example/scanapp/service/
git commit -m "feat: 实现Android平台扫描、位置和导出服务"
```

## 接口

- Consumes: Task 4的Android平台实现，Task 5的服务接口
- Produces: Android平台特定的服务实现

## 全局约束

- 目标平台：Android 8.0+、iOS 13.0+、鸿蒙2.0+
- 使用Kuikly KMP工程模板
- 数据库使用SQLDelight
- UI使用Kuikly DSL构建
- 后台扫描需要平台特定的后台任务管理