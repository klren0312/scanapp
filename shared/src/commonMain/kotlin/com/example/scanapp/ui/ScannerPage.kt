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