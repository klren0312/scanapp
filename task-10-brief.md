# Task 10: 测试和优化

## 任务描述

进行测试和优化，包括：
- 添加测试依赖
- 创建数据库测试
- 创建扫描器测试
- 性能优化

## 具体步骤

### Step 1: 添加测试依赖

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

### Step 2: 创建数据库测试

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

### Step 3: 创建扫描器测试

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

### Step 4: 性能优化

- 优化数据库查询
- 减少内存使用
- 优化电池消耗

### Step 5: 提交代码

```bash
git add shared/src/commonTest/
git commit -m "feat: 添加单元测试和性能优化"
```

## 接口

- Consumes: 所有先前任务
- Produces: 测试代码和优化后的应用

## 全局约束

- 目标平台：Android 8.0+、iOS 13.0+、鸿蒙2.0+
- 使用Kuikly KMP工程模板
- 数据库使用SQLDelight
- UI使用Kuikly DSL构建
- 后台扫描需要平台特定的后台任务管理