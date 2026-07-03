package com.example.scanapp.ui

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.size
import com.tencent.kuikly.core.layout.width
import com.tencent.kuikly.core.layout.height
import com.tencent.kuikly.core.layout.flexDirection
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.justifyContent
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.layout.alignItems
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.padding
import com.tencent.kuikly.core.layout.marginTop
import com.tencent.kuikly.core.layout.marginBottom
import com.tencent.kuikly.core.layout.marginRight
import com.tencent.kuikly.core.layout.flex
import com.tencent.kuikly.core.layout.backgroundColor
import com.tencent.kuikly.core.layout.borderRadius
import com.tencent.kuikly.core.layout.color
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.Button
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.reactive.variable
import com.tencent.kuikly.core.base.Page
import com.tencent.kuikly.core.base.Pager
import com.tencent.kuikly.core.base.annotation.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.module.RouterModule
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.models.WifiScanRecord
import com.example.scanapp.models.BluetoothScanRecord

@Page("Scanner")
class ScannerPage : Pager() {

    private val isScanning = variable(false)
    private val wifiCount = variable(0L)
    private val bluetoothCount = variable(0L)
    private val recentWifi = variable<List<WifiScanRecord>>(emptyList())
    private val recentBluetooth = variable<List<BluetoothScanRecord>>(emptyList())

    override fun created() {
        super.created()
        refreshData()
    }

    override fun body(): ViewContainer {
        return View {
            attr {
                size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                backgroundColor(Color.parse("#F5F5F5"))
                flexDirection(FlexDirection.COLUMN)
                padding(16f)
            }

            Text {
                attr {
                    text("WiFi/蓝牙扫描器")
                    fontSize(24f)
                    marginTop(40f)
                    alignSelf(FlexAlign.CENTER)
                    color(Color.parse("#333333"))
                }
            }

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_AROUND)
                    marginTop(20f)
                    width(pagerData.pageViewWidth - 32f)
                    padding(12f)
                    backgroundColor(Color.WHITE)
                    borderRadius(8f)
                }

                Text {
                    attr {
                        text("WiFi: ${wifiCount.value}")
                        fontSize(16f)
                        color(Color.parse("#2196F3"))
                    }
                }

                Text {
                    attr {
                        text("蓝牙: ${bluetoothCount.value}")
                        fontSize(16f)
                        color(Color.parse("#4CAF50"))
                    }
                }
            }

            Button {
                attr {
                    text(if (isScanning.value) "停止扫描" else "开始扫描")
                    marginTop(16f)
                    padding(12f, 0f)
                    width(pagerData.pageViewWidth - 32f)
                    backgroundColor(if (isScanning.value) Color.parse("#F44336") else Color.parse("#2196F3"))
                    borderRadius(8f)
                    color(Color.WHITE)
                    fontSize(18f)
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

            if (isScanning.value) {
                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        justifyContent(FlexJustifyContent.CENTER)
                        marginTop(10f)
                    }
                    Text {
                        attr {
                            text("扫描中...")
                            fontSize(14f)
                            color(Color.parse("#FF9800"))
                        }
                    }
                }
            }

            Text {
                attr {
                    text("最近扫描结果")
                    fontSize(16f)
                    marginTop(16f)
                    color(Color.parse("#333333"))
                }
            }

            Scroller {
                attr {
                    flex(1f)
                    width(pagerData.pageViewWidth - 32f)
                    marginTop(8f)
                }

                Text {
                    attr {
                        text("--- WiFi 设备 ---")
                        fontSize(14f)
                        color(Color.parse("#666666"))
                        marginTop(4f)
                    }
                }

                recentWifi.value.forEach { record ->
                    View {
                        attr {
                            padding(8f)
                            marginTop(4f)
                            backgroundColor(Color.WHITE)
                            borderRadius(6f)
                            flexDirection(FlexDirection.ROW)
                            justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                        }
                        Text {
                            attr {
                                text(record.ssid)
                                fontSize(14f)
                                color(Color.parse("#333333"))
                            }
                        }
                        Text {
                            attr {
                                text("${record.signalStrength}dBm")
                                fontSize(13f)
                                color(Color.parse("#999999"))
                            }
                        }
                    }
                }

                Text {
                    attr {
                        text("--- 蓝牙设备 ---")
                        fontSize(14f)
                        color(Color.parse("#666666"))
                        marginTop(12f)
                    }
                }

                recentBluetooth.value.forEach { record ->
                    View {
                        attr {
                            padding(8f)
                            marginTop(4f)
                            backgroundColor(Color.WHITE)
                            borderRadius(6f)
                            flexDirection(FlexDirection.ROW)
                            justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                        }
                        Text {
                            attr {
                                text(record.name)
                                fontSize(14f)
                                color(Color.parse("#333333"))
                            }
                        }
                        Text {
                            attr {
                                text("${record.rssi}dBm")
                                fontSize(13f)
                                color(Color.parse("#999999"))
                            }
                        }
                    }
                }
            }

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_AROUND)
                    padding(8f)
                    marginTop(8f)
                    backgroundColor(Color.WHITE)
                    borderRadius(8f)
                    width(pagerData.pageViewWidth - 32f)
                }

                Button {
                    attr {
                        text("设备列表")
                        fontSize(14f)
                        padding(8f)
                        color(Color.parse("#2196F3"))
                        backgroundColor(Color.parse("#E3F2FD"))
                        borderRadius(6f)
                    }
                    event {
                        click {
                            acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                                .openPage(pageName = "DeviceList")
                        }
                    }
                }

                Button {
                    attr {
                        text("统计")
                        fontSize(14f)
                        padding(8f)
                        color(Color.parse("#4CAF50"))
                        backgroundColor(Color.parse("#E8F5E9"))
                        borderRadius(6f)
                    }
                    event {
                        click {
                            acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                                .openPage(pageName = "Statistics")
                        }
                    }
                }

                Button {
                    attr {
                        text("地图")
                        fontSize(14f)
                        padding(8f)
                        color(Color.parse("#FF9800"))
                        backgroundColor(Color.parse("#FFF3E0"))
                        borderRadius(6f)
                    }
                    event {
                        click {
                            acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                                .openPage(pageName = "Map")
                        }
                    }
                }

                Button {
                    attr {
                        text("设置")
                        fontSize(14f)
                        padding(8f)
                        color(Color.parse("#9C27B0"))
                        backgroundColor(Color.parse("#F3E5F5"))
                        borderRadius(6f)
                    }
                    event {
                        click {
                            acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                                .openPage(pageName = "Settings")
                        }
                    }
                }
            }
        }
    }

    private fun startScanning() {
        isScanning.value = true
        lifecycleScope.launch {
            while (isScanning.value) {
                refreshData()
                kotlinx.coroutines.delay(3000)
            }
        }
    }

    private fun stopScanning() {
        isScanning.value = false
    }

    private fun refreshData() {
        lifecycleScope.launch {
            try {
                val db = DatabaseFactory.getDatabase()
                val wifiDao = WifiScanDao(db)
                val btDao = BluetoothScanDao(db)

                wifiCount.value = wifiDao.getCount()
                bluetoothCount.value = btDao.getCount()
                recentWifi.value = wifiDao.getRecordsPaginated(limit = 10, offset = 0)
                recentBluetooth.value = btDao.getRecordsPaginated(limit = 10, offset = 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
