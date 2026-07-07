package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("DeviceList")
class DeviceListPage : Pager() {

    private var selectedTab = 0
    private var wifiRecords: List<WifiScanRecord> = emptyList()
    private var bluetoothRecords: List<BluetoothScanRecord> = emptyList()

    override fun created() {
        super.created()
        loadData()
    }

    override fun body(): ViewContainer<*, *>.() -> Unit = {
        val root = this
        View {
            attr {
                size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                backgroundColor(Color("#F5F5F5"))
                flexDirection(FlexDirection.COLUMN)
                padding(16f)
            }

            TitleText("Devices")

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.CENTER)
                    marginTop(16f)
                }
                this@DeviceListPage.run { root.TabButton("WiFi", this@DeviceListPage.selectedTab == 0) { this@DeviceListPage.selectedTab = 0 } }
                this@DeviceListPage.run { root.TabButton("Bluetooth", this@DeviceListPage.selectedTab == 1) { this@DeviceListPage.selectedTab = 1 } }
            }

            Scroller {
                attr {
                    flex(1f)
                    marginTop(8f)
                }
                if (this@DeviceListPage.selectedTab == 0) {
                    this@DeviceListPage.wifiRecords
                        .sortedByDescending { it.signalStrength }
                        .forEach { this@DeviceListPage.run { root.DeviceItem(it.ssid, it.bssid, it.signalStrength, it.count) } }
                    if (this@DeviceListPage.wifiRecords.isEmpty()) InfoText("No WiFi records")
                } else {
                    this@DeviceListPage.bluetoothRecords
                        .sortedByDescending { it.rssi }
                        .forEach { this@DeviceListPage.run { root.DeviceItem(it.name, it.address, it.rssi, it.count) } }
                    if (this@DeviceListPage.bluetoothRecords.isEmpty()) InfoText("No Bluetooth records")
                }
            }
        }
    }

    private fun ViewContainer<*, *>.TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
        ActionButton(
            label = label,
            background = if (selected) Color("#2196F3") else Color.TRANSPARENT,
            textColor = if (selected) Color.WHITE else Color("#333333"),
            onClick = onClick
        )
    }

    private fun ViewContainer<*, *>.DeviceItem(name: String, address: String, signal: Int, count: Int) {
        View {
            attr {
                flexDirection(FlexDirection.COLUMN)
                padding(10f)
                marginTop(6f)
                backgroundColor(Color.WHITE)
                borderRadius(8f)
            }
            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                    alignItems(FlexAlign.CENTER)
                }
                Text {
                    attr {
                        text(name.ifEmpty { "Unknown" })
                        fontSize(15f)
                        color(Color("#333333"))
                    }
                }
                Text {
                    attr {
                        text("$signal dBm")
                        fontSize(12f)
                        color(Color("#999999"))
                    }
                }
            }
            InfoText(address, Color("#999999"))
            InfoText("Seen $count times", Color("#2196F3"))
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            runCatching {
                val db = DatabaseFactory.getDatabase()
                wifiRecords = WifiScanDao(db).getAllRecords()
                bluetoothRecords = BluetoothScanDao(db).getAllRecords()
            }.onFailure { it.printStackTrace() }
        }
    }
}
