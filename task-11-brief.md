# Task 11: 文档和部署

## 任务描述

创建项目文档，包括：
- README.md
- 用户指南
- 开发者指南

## 具体步骤

### Step 1: 创建README.md

```markdown
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
```

### Step 2: 创建用户指南

```markdown
# 用户指南

## 安装应用

1. 从Google Play Store下载应用（Android）
2. 从App Store下载应用（iOS）
3. 从AppGallery下载应用（鸿蒙）

## 使用方法

### 开始扫描

1. 打开应用
2. 点击"开始扫描"按钮
3. 应用将开始扫描附近的WiFi和蓝牙设备

### 查看设备列表

1. 点击"设备列表"按钮
2. 查看所有扫描到的设备

### 查看统计信息

1. 点击"统计信息"按钮
2. 查看扫描数据的统计图表

### 查看地图

1. 点击"地图视图"按钮
2. 在地图上查看设备位置

### 导出数据

1. 点击"设置"按钮
2. 选择"导出数据"
3. 选择导出格式（CSV/JSON）

## 权限说明

- 位置权限：用于获取GPS位置
- 蓝牙权限：用于扫描蓝牙设备
- WiFi权限：用于扫描WiFi网络
- 存储权限：用于保存导出文件
```

### Step 3: 创建开发者指南

```markdown
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
```

### Step 4: 提交文档

```bash
git add README.md docs/
git commit -m "feat: 添加项目文档"
```

## 接口

- Consumes: 所有先前任务
- Produces: 项目文档

## 全局约束

- 目标平台：Android 8.0+、iOS 13.0+、鸿蒙2.0+
- 使用Kuikly KMP工程模板
- 数据库使用SQLDelight
- UI使用Kuikly DSL构建
- 后台扫描需要平台特定的后台任务管理