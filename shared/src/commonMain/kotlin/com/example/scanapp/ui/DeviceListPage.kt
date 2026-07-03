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
import com.tencent.kuikly.core.layout.marginLeft
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
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.models.WifiScanRecord
import com.example.scanapp.models.BluetoothScanRecord

@Page("DeviceList")
class DeviceListPage : Pager() {

    private var selectedTab = variable(0)
    private val wifiRecords = variable<List<WifiScanRecord>>(emptyList())
    private val bluetoothRecords = variable<List<BluetoothScanRecord>>(emptyList())

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
                    text("设备列表")
                    fontSize(24f)
                    marginTop(40f)
                    alignSelf(FlexAlign.CENTER)
                    color(Color.parse("#333333"))
                }
            }

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.CENTER)
                    marginTop(16f)
                    width(pagerData.pageViewWidth - 32f)
                    backgroundColor(Color.WHITE)
                    borderRadius(8f)
                    padding(4f)
                }

                Button {
                    attr {
                        text("WiFi设备")
                        flex(1f)
                        padding(10f, 0f)
                        fontSize(15f)
                        color(if (selectedTab.value == 0) Color.WHITE else Color.parse("#333333"))
                        backgroundColor(if (selectedTab.value == 0) Color.parse("#2196F3") else Color.TRANSPARENT)
                        borderRadius(6f)
                    }
                    event {
                        click {
                            selectedTab.value = 0
                        }
                    }
                }

                Button {
                    attr {
                        text("蓝牙设备")
                        flex(1f)
                        padding(10f, 0f)
                        fontSize(15f)
                        color(if (selectedTab.value == 1) Color.WHITE else Color.parse("#333333"))
                        backgroundColor(if (selectedTab.value == 1) Color.parse("#4CAF50") else Color.TRANSPARENT)
                        borderRadius(6f)
                    }
                    event {
                        click {
                            selectedTab.value = 1
                        }
                    }
                }
            }

            if (selectedTab.value == 0) {
                Scroller {
                    attr {
                        flex(1f)
                        width(pagerData.pageViewWidth - 32f)
                        marginTop(8f)
                    }

                    wifiRecords.value
                        .sortedByDescending { it.signalStrength }
                        .forEach { record ->
                            DeviceItemView(
                                name = record.ssid,
                                address = record.bssid,
                                signalStrength = record.signalStrength,
                                count = record.count,
                                type = "WiFi"
                            )
                        }

                    if (wifiRecords.value.isEmpty()) {
                        Text {
                            attr {
                                text("暂无WiFi设备记录")
                                fontSize(14f)
                                color(Color.parse("#999999"))
                                alignSelf(FlexAlign.CENTER)
                                marginTop(40f)
                            }
                        }
                    }
                }
            }

            if (selectedTab.value == 1) {
                Scroller {
                    attr {
                        flex(1f)
                        width(pagerData.pageViewWidth - 32f)
                        marginTop(8f)
                    }

                    bluetoothRecords.value
                        .sortedByDescending { it.rssi }
                        .forEach { record ->
                            DeviceItemView(
                                name = record.name,
                                address = record.address,
                                signalStrength = record.rssi,
                                count = record.count,
                                type = "Bluetooth"
                            )
                        }

                    if (bluetoothRecords.value.isEmpty()) {
                        Text {
                            attr {
                                text("暂无蓝牙设备记录")
                                fontSize(14f)
                                color(Color.parse("#999999"))
                                alignSelf(FlexAlign.CENTER)
                                marginTop(40f)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val db = DatabaseFactory.getDatabase()
                wifiRecords.value = WifiScanDao(db).getAllRecords()
                bluetoothRecords.value = BluetoothScanDao(db).getAllRecords()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun ViewContainer.DeviceItemView(
        name: String,
        address: String,
        signalStrength: Int,
        count: Int,
        type: String
    ) {
        View {
            attr {
                flexDirection(FlexDirection.COLUMN)
                padding(10f)
                marginTop(6f)
                backgroundColor(Color.WHITE)
                borderRadius(8f)
                width(pagerData.pageViewWidth - 32f)
            }

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                    alignItems(FlexAlign.CENTER)
                }

                Text {
                    attr {
                        text(name.ifEmpty { "<未知>" })
                        fontSize(15f)
                        color(Color.parse("#333333"))
                    }
                }

                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        alignItems(FlexAlign.CENTER)
                    }

                    SignalStrengthBars(signalStrength)

                    Text {
                        attr {
                            text("${signalStrength}dBm")
                            fontSize(12f)
                            marginLeft(4f)
                            color(Color.parse("#999999"))
                        }
                    }
                }
            }

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                    marginTop(4f)
                }

                Text {
                    attr {
                        text(address)
                        fontSize(12f)
                        color(Color.parse("#999999"))
                    }
                }

                View {
                    attr {
                        backgroundColor(Color.parse("#E3F2FD"))
                        borderRadius(10f)
                        padding(3f, 8f)
                    }
                    Text {
                        attr {
                            text("${count}次")
                            fontSize(11f)
                            color(Color.parse("#2196F3"))
                        }
                    }
                }
            }
        }
    }

    private fun ViewContainer.SignalStrengthBars(strength: Int) {
        val bars = when {
            strength > -50 -> 5
            strength > -65 -> 4
            strength > -75 -> 3
            strength > -85 -> 2
            else -> 1
        }

        View {
            attr {
                flexDirection(FlexDirection.ROW)
                alignItems(FlexAlign.END)
                height(14f)
            }

            for (i in 1..5) {
                View {
                    attr {
                        width(4f)
                        height((4f + i * 2f))
                        marginRight(2f)
                        backgroundColor(
                            if (i <= bars) Color.parse("#4CAF50") else Color.parse("#E0E0E0")
                        )
                        borderRadius(2f)
                    }
                }
            }
        }
    }
}
