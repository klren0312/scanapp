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
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("DeviceList")
class DeviceListPage : Pager() {

    private var selectedTab by observable(0)
    private var deviceListText by observable("No WiFi records")
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

            MdcTopBar("Devices") { this@DeviceListPage.closePage() }

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.CENTER)
                    marginTop(MdcTheme.Spacing.sm)
                }
                this@DeviceListPage.run { root.MdcCustomTab("WiFi", this@DeviceListPage.selectedTab == 0) { this@DeviceListPage.selectTab(0) } }
                this@DeviceListPage.run { root.MdcCustomTab("Bluetooth", this@DeviceListPage.selectedTab == 1) { this@DeviceListPage.selectTab(1) } }
            }

            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }
                Text {
                    attr {
                        text(this@DeviceListPage.deviceListText)
                        fontSize(MdcTheme.Typography.bodyMedium)
                        color(MdcTheme.Colors.onSurface)
                        lineHeight(22f)
                    }
                }
            }
        }
    }

    private fun ViewContainer<*, *>.MdcCustomTab(label: String, selected: Boolean, onClick: () -> Unit) {
        MdcTab(label = label, selected = selected, onClick = onClick)
    }

    private fun loadData() {
        lifecycleScope.launch {
            runCatching {
                val db = DatabaseFactory.getDatabase()
                wifiRecords = WifiScanDao(db).getAllRecords().sortedByDescending { it.signalStrength }
                bluetoothRecords = BluetoothScanDao(db).getAllRecords().sortedByDescending { it.rssi }
                updateVisibleList()
            }.onFailure { it.printStackTrace() }
        }
    }

    private fun selectTab(tab: Int) {
        selectedTab = tab
        updateVisibleList()
    }

    private fun updateVisibleList() {
        deviceListText = if (selectedTab == 0) {
            formatWifiRecords(wifiRecords)
        } else {
            formatBluetoothRecords(bluetoothRecords)
        }
    }

    private fun formatWifiRecords(records: List<WifiScanRecord>): String {
        if (records.isEmpty()) return "No WiFi records"
        return records.joinToString(separator = "\n\n") {
            "${it.ssid.ifEmpty { "Unknown" }}  ${it.signalStrength} dBm\n${it.bssid}\nSeen ${it.count} times"
        }
    }

    private fun formatBluetoothRecords(records: List<BluetoothScanRecord>): String {
        if (records.isEmpty()) return "No Bluetooth records"
        return records.joinToString(separator = "\n\n") {
            "${it.name.ifEmpty { "Unknown" }}  ${it.rssi} dBm\n${it.address}\nSeen ${it.count} times"
        }
    }

    private fun closePage() {
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
    }
}
