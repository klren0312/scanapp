# Fix Task 1: 修复审查发现的问题

## 审查发现的问题

### Important (Should Fix)

1. **ohosApp模块是空的，但在settings.gradle.kts中被引用**
   - 文件：`settings.gradle.kts:17`
   - 问题：Gradle会失败，因为没有对应的`build.gradle.kts`
   - 解决方案：添加一个最小的`ohosApp/build.gradle.kts`或从settings.gradle.kts中移除`:ohosApp`

2. **gradle-wrapper.properties使用了本地文件路径**
   - 文件：`gradle/wrapper/gradle-wrapper.properties:3`
   - 问题：`distributionUrl=file\:///D:/1project/scanapp/gradle-8.2-bin.zip`在其他机器上会失败
   - 解决方案：改为远程URL `https\://services.gradle.org/distributions/gradle-8.2-bin.zip`

3. **任务内部文件被提交到仓库**
   - 文件：`task-1-brief.md`, `docs/superpowers/plans/...`, `docs/superpowers/specs/...`
   - 问题：这些是规范/计划文档，不应该在源代码树中
   - 解决方案：从git中移除这些文件，或者将它们添加到.gitignore

## 修复步骤

### Step 1: 创建ohosApp/build.gradle.kts

```kotlin
// ohosApp/build.gradle.kts
plugins {
    kotlin("multiplatform")
}

kotlin {
    ohos {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    sourceSets {
        val ohosMain by getting {
            dependencies {
                implementation(project(":shared"))
            }
        }
    }
}
```

### Step 2: 修复gradle-wrapper.properties

将`distributionUrl=file\:///D:/1project/scanapp/gradle-8.2-bin.zip`改为：
`distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip`

### Step 3: 处理任务内部文件

选项1：从git中移除
```bash
git rm task-1-brief.md
git rm -r docs/superpowers/
```

选项2：添加到.gitignore
```
# 任务文档
task-*.md
docs/superpowers/
```

### Step 4: 提交修复

```bash
git add .
git commit -m "fix: 修复Task 1审查发现的问题"
```