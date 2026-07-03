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
