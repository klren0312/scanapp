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
import com.tencent.kuikly.core.layout.alignSelf
import com.tencent.kuikly.core.layout.padding
import com.tencent.kuikly.core.layout.marginTop
import com.tencent.kuikly.core.layout.marginLeft
import com.tencent.kuikly.core.layout.marginRight
import com.tencent.kuikly.core.layout.flex
import com.tencent.kuikly.core.layout.backgroundColor
import com.tencent.kuikly.core.layout.borderRadius
import com.tencent.kuikly.core.layout.color
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.Button
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.Switch
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.reactive.variable
import com.tencent.kuikly.core.base.Page
import com.tencent.kuikly.core.base.Pager
import com.tencent.kuikly.core.base.annotation.Page
import com.tencent.kuikly.core.base.Color
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.LocationDao
import com.example.scanapp.service.ExportServiceImpl

@Page("Settings")
class SettingsPage : Pager() {

    private val scanInterval = variable("5")
    private val autoScan = variable(false)
    private val exportResult = variable("")

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
                    text("设置")
                    fontSize(24f)
                    marginTop(40f)
                    alignSelf(FlexAlign.CENTER)
                    color(Color.parse("#333333"))
                }
            }

            Scroller {
                attr {
                    flex(1f)
                    width(pagerData.pageViewWidth - 32f)
                    marginTop(12f)
                }

                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                        alignItems(FlexAlign.CENTER)
                        padding(14f)
                        backgroundColor(Color.WHITE)
                        borderRadius(8f)
                        width(pagerData.pageViewWidth - 32f)
                    }
                    Text {
                        attr {
                            text("扫描间隔")
                            fontSize(15f)
                            color(Color.parse("#333333"))
                        }
                    }
                    View {
                        attr {
                            flexDirection(FlexDirection.ROW)
                            alignItems(FlexAlign.CENTER)
                        }
                        Input {
                            attr {
                                width(60f)
                                height(36f)
                                padding(8f)
                                fontSize(14f)
                                text(scanInterval.value)
                                placeholder("秒")
                                backgroundColor(Color.parse("#F5F5F5"))
                                borderRadius(6f)
                                color(Color.parse("#333333"))
                            }
                            event {
                                textChanged { text ->
                                    scanInterval.value = text
                                }
                            }
                        }
                        Text {
                            attr {
                                text("秒")
                                fontSize(14f)
                                marginLeft(6f)
                                color(Color.parse("#999999"))
                            }
                        }
                    }
                }

                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                        alignItems(FlexAlign.CENTER)
                        padding(14f)
                        marginTop(10f)
                        backgroundColor(Color.WHITE)
                        borderRadius(8f)
                        width(pagerData.pageViewWidth - 32f)
                    }
                    Text {
                        attr {
                            text("自动扫描")
                            fontSize(15f)
                            color(Color.parse("#333333"))
                        }
                    }
                    Switch {
                        attr {
                            checked(autoScan.value)
                        }
                        event {
                            valueChanged { checked ->
                                autoScan.value = checked
                            }
                        }
                    }
                }

                Text {
                    attr {
                        text("导出数据")
                        fontSize(16f)
                        marginTop(20f)
                        color(Color.parse("#333333"))
                    }
                }

                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                        marginTop(8f)
                        width(pagerData.pageViewWidth - 32f)
                    }
                    Button {
                        attr {
                            text("导出CSV")
                            flex(1f)
                            padding(12f, 0f)
                            marginRight(6f)
                            fontSize(15f)
                            backgroundColor(Color.parse("#2196F3"))
                            color(Color.WHITE)
                            borderRadius(8f)
                        }
                        event {
                            click {
                                exportData("csv")
                            }
                        }
                    }
                    Button {
                        attr {
                            text("导出JSON")
                            flex(1f)
                            padding(12f, 0f)
                            marginLeft(6f)
                            fontSize(15f)
                            backgroundColor(Color.parse("#4CAF50"))
                            color(Color.WHITE)
                            borderRadius(8f)
                        }
                        event {
                            click {
                                exportData("json")
                            }
                        }
                    }
                }

                if (exportResult.value.isNotEmpty()) {
                    Text {
                        attr {
                            text(exportResult.value)
                            fontSize(13f)
                            marginTop(8f)
                            color(Color.parse("#4CAF50"))
                        }
                    }
                }

                Text {
                    attr {
                        text("数据管理")
                        fontSize(16f)
                        marginTop(20f)
                        color(Color.parse("#333333"))
                    }
                }

                Button {
                    attr {
                        text("清除所有数据")
                        width(pagerData.pageViewWidth - 32f)
                        padding(12f, 0f)
                        marginTop(8f)
                        fontSize(15f)
                        backgroundColor(Color.parse("#F44336"))
                        color(Color.WHITE)
                        borderRadius(8f)
                    }
                    event {
                        click {
                            clearAllData()
                        }
                    }
                }

                Text {
                    attr {
                        text("关于")
                        fontSize(16f)
                        marginTop(24f)
                        color(Color.parse("#333333"))
                    }
                }

                View {
                    attr {
                        padding(14f)
                        marginTop(8f)
                        backgroundColor(Color.WHITE)
                        borderRadius(8f)
                        width(pagerData.pageViewWidth - 32f)
                    }
                    Text {
                        attr {
                            text("ScanApp v1.0")
                            fontSize(15f)
                            color(Color.parse("#333333"))
                        }
                    }
                    Text {
                        attr {
                            text("WiFi/蓝牙扫描器 - 跨平台扫描工具")
                            fontSize(13f)
                            marginTop(4f)
                            color(Color.parse("#999999"))
                        }
                    }
                    Text {
                        attr {
                            text("基于 Kuikly KMP 构建")
                            fontSize(12f)
                            marginTop(2f)
                            color(Color.parse("#BBBBBB"))
                        }
                    }
                }
            }
        }
    }

    private fun exportData(format: String) {
        lifecycleScope.launch {
            try {
                val db = DatabaseFactory.getDatabase()
                val wifiDao = WifiScanDao(db)
                val btDao = BluetoothScanDao(db)
                val locDao = LocationDao(db)

                val wifiRecords = wifiDao.getAllRecords()
                val btRecords = btDao.getAllRecords()
                val locRecords = locDao.getAllRecords()

                val exporter = ExportServiceImpl()
                val result = if (format == "csv") {
                    exporter.exportToCsv(wifiRecords, btRecords, locRecords)
                } else {
                    exporter.exportToJson(wifiRecords, btRecords, locRecords)
                }

                exportResult.value = "${format.uppercase()} 导出成功 (${result.length} 字符)"
            } catch (e: Exception) {
                exportResult.value = "导出失败: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    private fun clearAllData() {
        lifecycleScope.launch {
            try {
                val db = DatabaseFactory.getDatabase()
                WifiScanDao(db).deleteAll()
                BluetoothScanDao(db).deleteAll()
                LocationDao(db).deleteAll()

                exportResult.value = "所有数据已清除"
            } catch (e: Exception) {
                exportResult.value = "清除失败: ${e.message}"
                e.printStackTrace()
            }
        }
    }
}
