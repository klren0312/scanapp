# Task 9: 实现后台扫描服务

## 任务描述

实现Android后台扫描服务，包括：
- 创建后台扫描Worker
- 创建后台扫描服务
- 在AndroidManifest.xml中注册服务
- 添加WorkManager依赖

## 具体步骤

### Step 1: 创建后台扫描Worker

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

### Step 2: 创建后台扫描服务

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

### Step 3: 在AndroidManifest.xml中注册服务

```xml
<service
    android:name=".BackgroundScanService"
    android:enabled="true"
    android:exported="false" />
```

### Step 4: 添加WorkManager依赖

在`androidApp/build.gradle.kts`中添加：
```kotlin
implementation("androidx.work:work-runtime:2.8.1")
implementation("androidx.work:work-runtime-ktx:2.8.1")
```

### Step 5: 提交代码

```bash
git add shared/src/androidMain/kotlin/com/example/scanapp/service/
git add androidApp/
git commit -m "feat: 实现Android后台扫描服务"
```

## 接口

- Consumes: Task 6的Android服务实现
- Produces: Android后台扫描服务

## 全局约束

- 目标平台：Android 8.0+、iOS 13.0+、鸿蒙2.0+
- 使用Kuikly KMP工程模板
- 数据库使用SQLDelight
- UI使用Kuikly DSL构建
- 后台扫描需要平台特定的后台任务管理