package com.example.scanapp.ui

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.size
import com.tencent.kuikly.core.layout.width
import com.tencent.kuikly.core.layout.height
import com.tencent.kuikly.core.layout.flexDirection
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.justifyContent
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.layout.alignSelf
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.padding
import com.tencent.kuikly.core.layout.marginTop
import com.tencent.kuikly.core.layout.marginBottom
import com.tencent.kuikly.core.layout.flex
import com.tencent.kuikly.core.layout.backgroundColor
import com.tencent.kuikly.core.layout.borderRadius
import com.tencent.kuikly.core.layout.color
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
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
import com.example.scanapp.models.WifiScanRecord
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.LocationRecord

@Page("Statistics")
class StatisticsPage : Pager() {

    private val totalWifi = variable(0L)
    private val totalBluetooth = variable(0L)
    private val totalLocations = variable(0L)
    private val topWifi = variable<List<WifiScanRecord>>(emptyList())
    private val topBluetooth = variable<List<BluetoothScanRecord>>(emptyList())

    override fun created() {
        super.created()
        loadData()
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
                    text("统计信息")
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

                StatCard(
                    title = "总计",
                    wifiValue = totalWifi.value,
                    btValue = totalBluetooth.value,
                    locValue = totalLocations.value
                )

                Text {
                    attr {
                        text("最常见的WiFi设备")
                        fontSize(16f)
                        marginTop(16f)
                        color(Color.parse("#333333"))
                    }
                }

                topWifi.value.forEachIndexed { index, record ->
                    View {
                        attr {
                            flexDirection(FlexDirection.ROW)
                            justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                            padding(10f)
                            marginTop(4f)
                            backgroundColor(Color.WHITE)
                            borderRadius(6f)
                            width(pagerData.pageViewWidth - 32f)
                        }
                        Text {
                            attr {
                                text("${index + 1}. ${record.ssid}")
                                fontSize(14f)
                                color(Color.parse("#333333"))
                            }
                        }
                        Text {
                            attr {
                                text("${record.count}次")
                                fontSize(13f)
                                color(Color.parse("#2196F3"))
                            }
                        }
                    }
                }

                if (topWifi.value.isEmpty()) {
                    Text {
                        attr {
                            text("暂无数据")
                            fontSize(13f)
                            color(Color.parse("#999999"))
                            marginTop(4f)
                        }
                    }
                }

                Text {
                    attr {
                        text("最常见的蓝牙设备")
                        fontSize(16f)
                        marginTop(16f)
                        color(Color.parse("#333333"))
                    }
                }

                topBluetooth.value.forEachIndexed { index, record ->
                    View {
                        attr {
                            flexDirection(FlexDirection.ROW)
                            justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                            padding(10f)
                            marginTop(4f)
                            backgroundColor(Color.WHITE)
                            borderRadius(6f)
                            width(pagerData.pageViewWidth - 32f)
                        }
                        Text {
                            attr {
                                text("${index + 1}. ${record.name}")
                                fontSize(14f)
                                color(Color.parse("#333333"))
                            }
                        }
                        Text {
                            attr {
                                text("${record.count}次")
                                fontSize(13f)
                                color(Color.parse("#4CAF50"))
                            }
                        }
                    }
                }

                if (topBluetooth.value.isEmpty()) {
                    Text {
                        attr {
                            text("暂无数据")
                            fontSize(13f)
                            color(Color.parse("#999999"))
                            marginTop(4f)
                        }
                    }
                }

                Text {
                    attr {
                        text("信号强度分布")
                        fontSize(16f)
                        marginTop(16f)
                        color(Color.parse("#333333"))
                    }
                }

                SignalDistributionCard(topWifi.value, "WiFi")
                SignalDistributionCard(topBluetooth.value, "蓝牙")

                Text {
                    attr {
                        text("位置记录数: ${totalLocations.value}")
                        fontSize(14f)
                        marginTop(16f)
                        marginBottom(20f)
                        color(Color.parse("#666666"))
                    }
                }
            }
        }
    }

    private fun ViewContainer.StatCard(
        title: String,
        wifiValue: Long,
        btValue: Long,
        locValue: Long
    ) {
        View {
            attr {
                flexDirection(FlexDirection.ROW)
                justifyContent(FlexJustifyContent.SPACE_AROUND)
                padding(16f)
                backgroundColor(Color.WHITE)
                borderRadius(8f)
                width(pagerData.pageViewWidth - 32f)
            }

            StatItem("WiFi", wifiValue, Color.parse("#2196F3"))
            StatItem("蓝牙", btValue, Color.parse("#4CAF50"))
            StatItem("位置", locValue, Color.parse("#FF9800"))
        }
    }

    private fun ViewContainer.StatItem(label: String, value: Long, labelColor: Color) {
        View {
            attr {
                alignItems(FlexAlign.CENTER)
            }
            Text {
                attr {
                    text("$value")
                    fontSize(28f)
                    color(labelColor)
                }
            }
            Text {
                attr {
                    text(label)
                    fontSize(13f)
                    marginTop(4f)
                    color(Color.parse("#999999"))
                }
            }
        }
    }

    private fun ViewContainer.SignalDistributionCard(
        records: List<WifiScanRecord>,
        label: String
    ) {
        val strong = records.count { it.signalStrength > -50 }
        val medium = records.count { it.signalStrength in -70..-50 }
        val weak = records.count { it.signalStrength < -70 }
        val total = records.size

        View {
            attr {
                padding(12f)
                marginTop(6f)
                backgroundColor(Color.WHITE)
                borderRadius(6f)
                width(pagerData.pageViewWidth - 32f)
            }
            if (total > 0) {
                Text {
                    attr {
                        text("$label - 强(>-50dBm): $strong")
                        fontSize(13f)
                        color(Color.parse("#333333"))
                    }
                }
                Text {
                    attr {
                        text("$label - 中(-50~-70dBm): $medium")
                        fontSize(13f)
                        marginTop(3f)
                        color(Color.parse("#333333"))
                    }
                }
                Text {
                    attr {
                        text("$label - 弱(<-70dBm): $weak")
                        fontSize(13f)
                        marginTop(3f)
                        color(Color.parse("#333333"))
                    }
                }
            } else {
                Text {
                    attr {
                        text("$label 暂无信号数据")
                        fontSize(13f)
                        color(Color.parse("#999999"))
                    }
                }
            }
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val db = DatabaseFactory.getDatabase()
                val wifiDao = WifiScanDao(db)
                val btDao = BluetoothScanDao(db)
                val locDao = LocationDao(db)

                totalWifi.value = wifiDao.getCount()
                totalBluetooth.value = btDao.getCount()
                totalLocations.value = locDao.getCount()

                val allWifi = wifiDao.getAllRecords()
                val allBt = btDao.getAllRecords()

                topWifi.value = allWifi
                    .sortedByDescending { it.count }
                    .take(5)

                topBluetooth.value = allBt
                    .sortedByDescending { it.count }
                    .take(5)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
