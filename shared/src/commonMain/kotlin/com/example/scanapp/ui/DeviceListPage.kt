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
                backgroundColor(MdcTheme.Colors.background)
                flexDirection(FlexDirection.COLUMN)
                padding(MdcTheme.Spacing.md)
            }

            MdcTitle("Devices")

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.CENTER)
                    marginTop(MdcTheme.Spacing.sm)
                }
                this@DeviceListPage.run { root.MdcCustomTab("WiFi", this@DeviceListPage.selectedTab == 0) { this@DeviceListPage.selectedTab = 0 } }
                this@DeviceListPage.run { root.MdcCustomTab("Bluetooth", this@DeviceListPage.selectedTab == 1) { this@DeviceListPage.selectedTab = 1 } }
            }

            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }
                if (this@DeviceListPage.selectedTab == 0) {
                    this@DeviceListPage.wifiRecords
                        .sortedByDescending { it.signalStrength }
                        .forEach { this@DeviceListPage.run { root.MdcDeviceCard(it.ssid, it.bssid, it.signalStrength, it.count) } }
                    if (this@DeviceListPage.wifiRecords.isEmpty()) MdcBodyText("No WiFi records", MdcTheme.Colors.onSurfaceVariant)
                } else {
                    this@DeviceListPage.bluetoothRecords
                        .sortedByDescending { it.rssi }
                        .forEach { this@DeviceListPage.run { root.MdcDeviceCard(it.name, it.address, it.rssi, it.count) } }
                    if (this@DeviceListPage.bluetoothRecords.isEmpty()) MdcBodyText("No Bluetooth records", MdcTheme.Colors.onSurfaceVariant)
                }
            }
        }
    }

    private fun ViewContainer<*, *>.MdcCustomTab(label: String, selected: Boolean, onClick: () -> Unit) {
        MdcTab(label = label, selected = selected, onClick = onClick)
    }

    private fun ViewContainer<*, *>.MdcDeviceCard(name: String, address: String, signal: Int, count: Int) {
        MdcCard {
            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                    alignItemsCenter()
                }
                Text {
                    attr {
                        text(name.ifEmpty { "Unknown" })
                        fontSize(MdcTheme.Typography.bodyLarge)
                        fontWeightMedium()
                        color(MdcTheme.Colors.onSurface)
                    }
                }
                Text {
                    attr {
                        text("$signal dBm")
                        fontSize(MdcTheme.Typography.bodySmall)
                        color(MdcTheme.Colors.onSurfaceVariant)
                    }
                }
            }
            MdcCaption(address)
            Text {
                attr {
                    text("Seen $count times")
                    fontSize(MdcTheme.Typography.bodySmall)
                    color(MdcTheme.Colors.primary)
                    marginTop(4f)
                }
            }
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
