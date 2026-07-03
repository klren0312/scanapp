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
