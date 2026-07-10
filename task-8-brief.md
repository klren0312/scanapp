# Task 8: 配置Android壳工程

## 任务描述

配置Android壳工程，包括：
- 添加Kuikly渲染器依赖
- 实现Kuikly渲染Activity
- 修改MainActivity
- 配置FileProvider

## 具体步骤

### Step 1: 添加Kuikly渲染器依赖

在`androidApp/build.gradle.kts`中添加：
```kotlin
dependencies {
    implementation("com.tencent.kuikly-open:core-render-android:2.0.0")
    implementation("com.tencent.kuikly-open:core:2.0.0")
    implementation(project(":shared"))
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
}
```

### Step 2: 实现Kuikly渲染Activity

```kotlin
// androidApp/src/main/java/com/example/scanapp/KuiklyRenderActivity.kt
package com.example.scanapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tencent.kuikly.render.core.KuiklyView
import com.tencent.kuikly.render.core.KuiklyViewDelegator

class KuiklyRenderActivity : AppCompatActivity() {
    
    private lateinit var kuiklyViewDelegator: KuiklyViewDelegator
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val pageName = intent.getStringExtra("pageName") ?: ""
        
        kuiklyViewDelegator = KuiklyViewDelegator(
            this,
            KuiklyView.CodeHandler(pageName)
        )
        
        setContentView(kuiklyViewDelegator.kuiklyView)
        
        kuiklyViewDelegator.loadPage(pageName)
    }
    
    companion object {
        fun start(activity: android.app.Activity, pageName: String) {
            val intent = android.content.Intent(activity, KuiklyRenderActivity::class.java)
            intent.putExtra("pageName", pageName)
            activity.startActivity(intent)
        }
    }
}
```

### Step 3: 修改MainActivity

```kotlin
// androidApp/src/main/java/com/example/scanapp/MainActivity.kt
package com.example.scanapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout
import android.view.ViewGroup
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(50, 50, 50, 50)
        }
        
        val titleText = TextView(this).apply {
            text = "WiFi/蓝牙扫描器"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val scanButton = Button(this).apply {
            text = "开始扫描"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 50
            }
            setOnClickListener {
                KuiklyRenderActivity.start(this@MainActivity, "Scanner")
            }
        }
        
        val deviceListButton = Button(this).apply {
            text = "设备列表"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
            setOnClickListener {
                KuiklyRenderActivity.start(this@MainActivity, "DeviceList")
            }
        }
        
        val statisticsButton = Button(this).apply {
            text = "统计信息"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
            setOnClickListener {
                KuiklyRenderActivity.start(this@MainActivity, "Statistics")
            }
        }
        
        val mapButton = Button(this).apply {
            text = "地图视图"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
            setOnClickListener {
                KuiklyRenderActivity.start(this@MainActivity, "Map")
            }
        }
        
        val settingsButton = Button(this).apply {
            text = "设置"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
            setOnClickListener {
                KuiklyRenderActivity.start(this@MainActivity, "Settings")
            }
        }
        
        layout.addView(titleText)
        layout.addView(scanButton)
        layout.addView(deviceListButton)
        layout.addView(statisticsButton)
        layout.addView(mapButton)
        layout.addView(settingsButton)
        
        setContentView(layout)
    }
}
```

### Step 4: 配置FileProvider

在`androidApp/src/main/res/xml/`目录下创建`file_paths.xml`：
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path name="external_files" path="."/>
    <cache-path name="cache" path="."/>
</paths>
```

### Step 5: 在AndroidManifest.xml中添加FileProvider

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### Step 6: 提交代码

```bash
git add androidApp/
git commit -m "feat: 配置Android壳工程，实现主界面和Kuikly渲染"
```

## 接口

- Consumes: Task 7的UI页面
- Produces: 可运行的Android应用

## 全局约束

- 目标平台：Android 8.0+、iOS 13.0+、鸿蒙2.0+
- 使用Kuikly KMP工程模板
- 数据库使用SQLDelight
- UI使用Kuikly DSL构建
- 后台扫描需要平台特定的后台任务管理