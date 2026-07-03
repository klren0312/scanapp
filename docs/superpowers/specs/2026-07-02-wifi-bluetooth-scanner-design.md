# WiFi/Bluetooth扫描器应用设计文档

## 1. 项目概述

### 1.1 项目目的
这是一个研究项目，用于扫描附近的WiFi和蓝牙设备，记录GPS位置，保存到本地SQLite数据库，并支持数据导出。

### 1.2 目标平台
- Android
- iOS
- 鸿蒙

### 1.3 核心功能
- 后台持续扫描WiFi和蓝牙设备
- 记录每次扫描的GPS位置
- 本地SQLite存储，重复设备增加计数
- 数据导出为CSV/JSON格式
- 可视化界面（列表、图表、地图）

## 2. 架构设计

### 2.1 架构方法
采用**模块化架构**，分离关注点，便于维护和扩展。

### 2.2 项目结构

```
scanapp/
├── shared/                    # 主模块（跨平台业务逻辑）
│   ├── scanner/              # WiFi和蓝牙扫描模块
│   ├── database/             # SQLite数据库模块
│   ├── location/             # GPS定位模块
│   ├── export/               # 数据导出模块
│   └── ui/                   # 用户界面模块
├── androidApp/               # Android壳工程
├── iosApp/                   # iOS壳工程
├── ohosApp/                  # 鸿蒙壳工程
└── docs/                     # 文档目录
```

### 2.3 核心依赖
- Kuikly UI框架
- Kotlin Multiplatform
- SQLDelight（跨平台SQLite）
- 原生平台API（WiFi、蓝牙、GPS）

## 3. 数据模型

### 3.1 WiFi扫描记录
```kotlin
data class WifiScanRecord(
    val id: Long,                    // 主键
    val ssid: String,                // WiFi名称
    val bssid: String,               // MAC地址
    val signalStrength: Int,         // 信号强度
    val frequency: Int,              // 频率
    val timestamp: Long,             // 扫描时间戳
    val latitude: Double,            // 纬度
    val longitude: Double,           // 经度
    val count: Int                   // 重复计数
)
```

### 3.2 蓝牙扫描记录
```kotlin
data class BluetoothScanRecord(
    val id: Long,                    // 主键
    val name: String,                // 设备名称
    val address: String,             // MAC地址
    val rssi: Int,                   // 信号强度
    val deviceType: String,          // 设备类型
    val timestamp: Long,             // 扫描时间戳
    val latitude: Double,            // 纬度
    val longitude: Double,           // 经度
    val count: Int                   // 重复计数
)
```

### 3.3 GPS位置记录
```kotlin
data class LocationRecord(
    val id: Long,                    // 主键
    val latitude: Double,            // 纬度
    val longitude: Double,           // 经度
    val altitude: Double,            // 海拔
    val accuracy: Float,             // 精度
    val timestamp: Long              // 记录时间戳
)
```

## 4. 功能模块

### 4.1 扫描模块（scanner）
- **WiFi扫描**：使用原生平台API扫描附近WiFi网络
- **蓝牙扫描**：使用BLE API扫描附近蓝牙设备
- **后台扫描**：支持应用在后台持续扫描
- **扫描控制**：开始、停止、暂停扫描

### 4.2 数据库模块（database）
- **SQLite存储**：使用SQLDelight进行跨平台数据库操作
- **数据去重**：通过MAC地址识别重复设备
- **计数功能**：重复扫描同一设备时增加计数
- **查询功能**：支持按时间、位置、设备类型查询

### 4.3 定位模块（location）
- **GPS定位**：获取当前经纬度、海拔、精度
- **位置更新**：定期更新位置信息
- **位置缓存**：缓存最近位置以减少GPS请求

### 4.4 导出模块（export）
- **CSV导出**：导出为CSV格式文件
- **JSON导出**：导出为JSON格式文件
- **分享功能**：支持分享到其他应用

### 4.5 UI模块（ui）
- **扫描界面**：显示实时扫描状态和结果
- **列表界面**：展示所有扫描记录
- **统计界面**：显示图表和统计数据
- **地图界面**：在地图上显示设备位置
- **设置界面**：配置扫描参数和导出选项

## 5. 技术实现

### 5.1 Kuikly框架集成
- 使用Kuikly KMP工程模板
- 集成Kuikly UI渲染器
- 使用Kuikly路由系统

### 5.2 跨平台实现

#### Android平台
- **WiFi**：WifiManager
- **蓝牙**：BluetoothLeScanner
- **GPS**：FusedLocationProviderClient
- **后台服务**：WorkManager

#### iOS平台
- **WiFi**：NEHotspotNetwork
- **蓝牙**：CBCentralManager
- **GPS**：CLLocationManager
- **后台任务**：BGTaskScheduler

#### 鸿蒙平台
- **WiFi**：WifiManager
- **蓝牙**：BluetoothManager
- **GPS**：LocationManager
- **后台任务**：BackgroundTaskManager

### 5.3 数据库实现
- 使用SQLDelight进行跨平台SQLite操作
- 定义SQL schema和查询
- 实现数据访问对象（DAO）

### 5.4 UI实现
- 使用Kuikly DSL构建UI组件
- 实现响应式数据绑定
- 支持深色模式

### 5.5 权限处理
- **Android**：声明必要权限（ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE, BLUETOOTH_SCAN等）
- **iOS**：请求位置、蓝牙权限
- **鸿蒙**：声明必要权限

## 6. 错误处理

### 6.1 错误类型
- **权限错误**：当权限被拒绝时的处理
- **硬件错误**：当设备不支持某些功能时的处理
- **数据库错误**：数据读写失败时的处理
- **网络错误**：导出功能需要网络时的处理

### 6.2 用户反馈
- 友好的错误提示
- 恢复建议
- 重试机制

## 7. 测试策略

### 7.1 单元测试
- 测试各个模块的核心逻辑
- 测试数据模型和数据库操作

### 7.2 集成测试
- 测试模块间的交互
- 测试扫描、存储、导出流程

### 7.3 平台测试
- 在真实设备上测试原生功能
- 测试后台扫描的稳定性

### 7.4 UI测试
- 测试用户界面交互
- 测试响应式数据绑定

### 7.5 性能测试
- 测试后台扫描的电池消耗
- 测试数据库查询性能

## 8. 日志和监控

### 8.1 本地日志
- 记录关键操作和错误
- 支持日志级别配置

### 8.2 性能监控
- 跟踪扫描频率和资源使用
- 监控数据库大小

### 8.3 崩溃报告
- 收集崩溃信息用于调试
- 支持崩溃重现

## 9. 数据安全

### 9.1 本地加密
- 敏感数据加密存储
- 使用平台提供的加密API

### 9.2 权限最小化
- 只请求必要的权限
- 提供权限说明

### 9.3 数据清理
- 提供清除数据的功能
- 支持选择性清理

## 10. 实现计划

### 10.1 阶段一：基础框架
- 创建Kuikly KMP工程
- 实现基础模块结构
- 集成SQLDelight

### 10.2 阶段二：核心功能
- 实现WiFi扫描
- 实现蓝牙扫描
- 实现GPS定位
- 实现数据存储

### 10.3 阶段三：高级功能
- 实现后台扫描
- 实现数据导出
- 实现UI界面

### 10.4 阶段四：优化和完善
- 性能优化
- 错误处理完善
- 测试和调试

## 11. 验收标准

### 11.1 功能验收
- [ ] WiFi扫描功能正常
- [ ] 蓝牙扫描功能正常
- [ ] GPS定位功能正常
- [ ] 数据存储和查询正常
- [ ] 重复设备计数正确
- [ ] 数据导出功能正常
- [ ] UI界面响应正常

### 11.2 性能验收
- [ ] 后台扫描电池消耗在可接受范围内
- [ ] 数据库查询响应时间<100ms
- [ ] UI界面流畅无卡顿

### 11.3 兼容性验收
- [ ] Android 8.0+ 设备正常运行
- [ ] iOS 13.0+ 设备正常运行
- [ ] 鸿蒙2.0+ 设备正常运行

---

**文档版本**：1.0  
**创建日期**：2026-07-02  
**作者**：AI助手  
**状态**：待用户审查