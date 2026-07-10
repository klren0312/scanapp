# Task 2: 定义数据模型

## 任务描述

创建数据模型类，包括：
- WiFi扫描记录模型
- 蓝牙扫描记录模型
- GPS位置记录模型

## 具体步骤

### Step 1: 创建WiFi扫描记录模型

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

### Step 2: 创建蓝牙扫描记录模型

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

### Step 3: 创建GPS位置记录模型

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

### Step 4: 提交代码

```bash
git add shared/src/commonMain/kotlin/com/example/scanapp/models/
git commit -m "feat: 添加数据模型定义"
```

## 接口

- Consumes: 无
- Produces: 数据模型类，供后续任务使用

## 全局约束

- 目标平台：Android 8.0+、iOS 13.0+、鸿蒙2.0+
- 使用Kuikly KMP工程模板
- 数据库使用SQLDelight
- UI使用Kuikly DSL构建
- 后台扫描需要平台特定的后台任务管理