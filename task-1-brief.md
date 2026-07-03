# Task 1: 创建Kuikly KMP工程

## 任务描述

创建Kuikly KMP工程基础结构，包括：
- shared模块（跨平台业务逻辑）
- androidApp模块（Android壳工程）
- iosApp模块（iOS壳工程）
- ohosApp模块（鸿蒙壳工程）

## 具体步骤

### Step 1: 使用Android Studio创建Kuikly工程

打开Android Studio，选择 File -> New -> New Project -> Kuikly Project Template
- 项目名称：scanapp
- 包名：com.example.scanapp
- 最低支持版本：Android 8.0、iOS 13.0
- 选择平台：Android、iOS、HarmonyOS
- DSL类型：Kuikly DSL

### Step 2: 验证工程结构

检查工程目录结构：
```
scanapp/
├── shared/
│   └── src/
│       ├── commonMain/
│       ├── androidMain/
│       ├── iosMain/
│       └── ohosArm64Main/
├── androidApp/
├── iosApp/
└── ohosApp/
```

### Step 3: 配置共享模块依赖

在`shared/build.gradle.kts`中添加KSP插件：
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

ksp {
    arg("moduleId", "shared")
}

dependencies {
    add("kspCommonMainMetadata", "com.tencent.kuikly:core-ksp:2.0.0")
}
```

### Step 4: 编译工程

运行Gradle同步：
```bash
./gradlew build
```
预期：BUILD SUCCESSFUL

### Step 5: 提交代码

```bash
git init
git add .
git commit -m "feat: 创建Kuikly KMP工程基础结构"
```

## 接口

- Consumes: 无
- Produces: 基础工程结构，包含Kuikly框架依赖

## 全局约束

- 目标平台：Android 8.0+、iOS 13.0+、鸿蒙2.0+
- 使用Kuikly KMP工程模板
- 数据库使用SQLDelight
- UI使用Kuikly DSL构建
- 后台扫描需要平台特定的后台任务管理