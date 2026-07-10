# Task 5: 实现跨平台扫描服务

## 完成内容

实现了跨平台扫描服务接口，包括：

1. **扫描服务接口** (`ScannerService.kt`)
   - 定义了WiFi和蓝牙扫描的基本操作
   - 使用StateFlow暴露扫描状态和设备列表
   - 支持单独和批量扫描控制

2. **位置服务接口** (`LocationService.kt`)
   - 定义了位置跟踪的基本操作
   - 使用StateFlow暴露当前位置和跟踪状态

3. **导出服务接口** (`ExportService.kt`)
   - 定义了CSV和JSON导出功能
   - 支持文件分享功能

4. **跨平台扫描服务实现** (`ScannerServiceImpl.kt`)
   - 使用依赖注入方式接收平台特定扫描函数
   - 自动将扫描结果保存到数据库
   - 支持定时扫描（每5秒）

5. **导出服务实现** (`ExportServiceImpl.kt`)
   - 实现了CSV格式导出（带CSV转义处理）
   - 实现了JSON格式导出（带JSON转义处理）
   - 使用手动JSON序列化避免额外依赖

## 文件变更

- `shared/src/commonMain/kotlin/com/example/scanapp/service/ScannerService.kt` (新建)
- `shared/src/commonMain/kotlin/com/example/scanapp/service/LocationService.kt` (新建)
- `shared/src/commonMain/kotlin/com/example/scanapp/service/ExportService.kt` (新建)
- `shared/src/commonMain/kotlin/com/example/scanapp/service/ScannerServiceImpl.kt` (新建)
- `shared/src/commonMain/kotlin/com/example/scanapp/service/ExportServiceImpl.kt` (新建)

## 设计说明

- **ScannerServiceImpl** 使用lambda函数注入平台特定扫描逻辑，保持common代码的平台无关性
- **ExportServiceImpl** 手动实现JSON序列化，避免引入kotlinx.serialization依赖
- 所有接口均使用Kotlin协程和StateFlow，符合KMP最佳实践

## 自我审查

- ✅ 完成了任务要求的所有接口和实现
- ✅ 代码遵循现有项目结构和命名规范
- ✅ 使用了正确的数据模型和DAO接口
- ✅ 实现了CSV和JSON导出功能，包含必要的转义处理
- ⚠️ 注意：`ExportService.shareFile()` 和 `ScannerServiceImpl` 中的平台特定扫描需要在 expect/actual 机制中完成

## 提交信息

- Commit: 12ee4dc
- Message: feat: implement cross-platform scanning, location, and export service interfaces
