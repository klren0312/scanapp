# Task 7: 实现Kuikly UI界面

## 任务描述

实现Kuikly UI界面，包括：
- 扫描页面
- 设备列表页面
- 统计页面
- 地图页面
- 设置页面

## 具体步骤

### Step 1: 创建扫描页面

```kotlin
// shared/src/commonMain/kotlin/com/example/scanapp/ui/ScannerPage.kt
package com.example.scanapp.ui

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.size
import com.tencent.kuikly.core.layout.width
import com.tencent.kuikly.core.layout.height
import com.tencent.kuikly.core.layout.allCenter
import com.tencent.kuikly.core.layout.padding
import com.tencent.kuikly.core.layout.marginTop
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.Button
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.reactive.variable
import com.tencent.kuikly.core.base.Page
import com.tencent.kuikly.core.base.PageParams
import com.tencent.kuikly.core.base.Pager
import com.tencent.kuikly.core.base.annotation.Page

@Page("Scanner")
class ScannerPage : Pager() {
    
    private val isScanning = variable(false)
    private val wifiCount = variable(0)
    private val bluetoothCount = variable(0)
    
    override fun body(): ViewContainer {
        return View {
            attr {
                allCenter()
                size(375f, 667f)
            }
            
            Text {
                attr {
                    text("WiFi/蓝牙扫描器")
                    fontSize(24f)
                    marginTop(50f)
                }
            }
            
            Text {
                attr {
                    text("WiFi设备: ${wifiCount.value}")
                    fontSize(16f)
                    marginTop(20f)
                }
            }
            
            Text {
                attr {
                    text("蓝牙设备: ${bluetoothCount.value}")
                    fontSize(16f)
                    marginTop(10f)
                }
            }
            
            Button {
                attr {
                    text(if (isScanning.value) "停止扫描" else "开始扫描")
                    marginTop(30f)
                    padding(10f, 20f)
                }
                event {
                    click {
                        if (isScanning.value) {
                            stopScanning()
                        } else {
                            startScanning()
                        }
                    }
                }
            }
        }
    }
    
    private fun startScanning() {
        isScanning.value = true
        // 启动扫描服务
    }
    
    private fun stopScanning() {
        isScanning.value = false
        // 停止扫描服务
    }
}
```

### Step 2: 创建设备列表页面

```kotlin
// shared/src/commonMain/kotlin/com/example/scanapp/ui/DeviceListPage.kt
package com.example.scanapp.ui

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.size
import com.tencent.kuikly.core.layout.allCenter
import com.tencent.kuikly.core.layout.padding
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.base.Page
import com.tencent.kuikly.core.base.Pager
import com.tencent.kuikly.core.base.annotation.Page

@Page("DeviceList")
class DeviceListPage : Pager() {
    
    override fun body(): ViewContainer {
        return View {
            attr {
                allCenter()
                size(375f, 667f)
            }
            
            Text {
                attr {
                    text("设备列表")
                    fontSize(24f)
                    marginTop(50f)
                }
            }
            
            // 设备列表将在这里实现
        }
    }
}
```

### Step 3: 创建统计页面

```kotlin
// shared/src/commonMain/kotlin/com/example/scanapp/ui/StatisticsPage.kt
package com.example.scanapp.ui

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.size
import com.tencent.kuikly.core.layout.allCenter
import com.tencent.kuikly.core.layout.padding
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.base.Page
import com.tencent.kuikly.core.base.Pager
import com.tencent.kuikly.core.base.annotation.Page

@Page("Statistics")
class StatisticsPage : Pager() {
    
    override fun body(): ViewContainer {
        return View {
            attr {
                allCenter()
                size(375f, 667f)
            }
            
            Text {
                attr {
                    text("统计信息")
                    fontSize(24f)
                    marginTop(50f)
                }
            }
            
            // 统计图表将在这里实现
        }
    }
}
```

### Step 4: 创建地图页面

```kotlin
// shared/src/commonMain/kotlin/com/example/scanapp/ui/MapPage.kt
package com.example.scanapp.ui

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.size
import com.tencent.kuikly.core.layout.allCenter
import com.tencent.kuikly.core.layout.padding
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.base.Page
import com.tencent.kuikly.core.base.Pager
import com.tencent.kuikly.core.base.annotation.Page

@Page("Map")
class MapPage : Pager() {
    
    override fun body(): ViewContainer {
        return View {
            attr {
                allCenter()
                size(375f, 667f)
            }
            
            Text {
                attr {
                    text("地图视图")
                    fontSize(24f)
                    marginTop(50f)
                }
            }
            
            // 地图将在这里实现
        }
    }
}
```

### Step 5: 创建设置页面

```kotlin
// shared/src/commonMain/kotlin/com/example/scanapp/ui/SettingsPage.kt
package com.example.scanapp.ui

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.size
import com.tencent.kuikly.core.layout.allCenter
import com.tencent.kuikly.core.layout.padding
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.base.Page
import com.tencent.kuikly.core.base.Pager
import com.tencent.kuikly.core.base.annotation.Page

@Page("Settings")
class SettingsPage : Pager() {
    
    override fun body(): ViewContainer {
        return View {
            attr {
                allCenter()
                size(375f, 667f)
            }
            
            Text {
                attr {
                    text("设置")
                    fontSize(24f)
                    marginTop(50f)
                }
            }
            
            // 设置选项将在这里实现
        }
    }
}
```

### Step 6: 提交代码

```bash
git add shared/src/commonMain/kotlin/com/example/scanapp/ui/
git commit -m "feat: 实现Kuikly UI界面"
```

## 接口

- Consumes: Task 5的服务接口
- Produces: Kuikly UI页面

## 全局约束

- 目标平台：Android 8.0+、iOS 13.0+、鸿蒙2.0+
- 使用Kuikly KMP工程模板
- 数据库使用SQLDelight
- UI使用Kuikly DSL构建
- 后台扫描需要平台特定的后台任务管理