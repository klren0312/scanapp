package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.coroutines.delay
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.View

@Page("DeviceList")
class DeviceListPage : Pager() {

    private var wifiDeviceCount by observable(0)
    private var bluetoothDeviceCount by observable(0)
    private var wifiSeenTotal by observable(0)
    private var bluetoothSeenTotal by observable(0)
    private var drawerOpen by observable(false)
    private var wifiRecords by observableList<WifiScanRecord>()
    private var bluetoothRecords by observableList<BluetoothScanRecord>()

    override fun created() {
        super.created()
        refreshData()
        lifecycleScope.launch {
            while (true) {
                delay(3000)
                refreshData()
            }
        }
    }

    override fun body(): ViewContainer<*, *>.() -> Unit = {
        View {
            attr {
                size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                backgroundColor(MdcTheme.Colors.background)
                flexDirection(FlexDirection.COLUMN)
                padding(MdcTheme.Spacing.md)
            }

            MdcMenuTopBar("Devices") { this@DeviceListPage.drawerOpen = true }

            MdcCardRow {
                MdcStatBadge("WiFi Devices", { "${this@DeviceListPage.wifiDeviceCount}" }, MdcTheme.Colors.wifi)
                MdcStatBadge("Bluetooth Devices", { "${this@DeviceListPage.bluetoothDeviceCount}" }, MdcTheme.Colors.bluetooth)
            }

            MdcCardRow(elevation = MdcTheme.Elevation.level0) {
                MdcStatBadge("WiFi Seen", { "${this@DeviceListPage.wifiSeenTotal}" }, MdcTheme.Colors.wifi)
                MdcStatBadge("Bluetooth Seen", { "${this@DeviceListPage.bluetoothSeenTotal}" }, MdcTheme.Colors.bluetooth)
            }

            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }
                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                    }
                    View {
                        attr {
                            flex(1f)
                            flexDirection(FlexDirection.COLUMN)
                            marginRight(MdcTheme.Spacing.sm)
                        }
                        MdcSectionHeader("WiFi")
                        vfor({ this@DeviceListPage.wifiRecords }) { record ->
                            MdcDeviceCard(
                                title = record.ssid.ifEmpty { "Unknown" },
                                identity = record.bssid,
                                primaryMetric = "${record.signalStrength} dBm",
                                secondaryMetric = "${record.frequency} MHz",
                                count = record.count,
                                color = MdcTheme.Colors.wifi
                            ) {
                                this@DeviceListPage.openDeviceDetail("wifi", record.bssid)
                            }
                        }
                        vif({ this@DeviceListPage.wifiDeviceCount == 0 }) {
                            MdcBodyText("No WiFi records", MdcTheme.Colors.onSurfaceVariant)
                        }
                    }

                    View {
                        attr {
                            flex(1f)
                            flexDirection(FlexDirection.COLUMN)
                        }
                        MdcSectionHeader("Bluetooth")
                        vfor({ this@DeviceListPage.bluetoothRecords }) { record ->
                            MdcDeviceCard(
                                title = record.name.ifEmpty { "Unknown" },
                                identity = record.address,
                                primaryMetric = "${record.rssi} dBm",
                                secondaryMetric = record.deviceType,
                                count = record.count,
                                color = MdcTheme.Colors.bluetooth
                            ) {
                                this@DeviceListPage.openDeviceDetail("bluetooth", record.address)
                            }
                        }
                        vif({ this@DeviceListPage.bluetoothDeviceCount == 0 }) {
                            MdcBodyText("No Bluetooth records", MdcTheme.Colors.onSurfaceVariant)
                        }
                    }
                }
            }

            MdcNavigationDrawerHost(
                isOpen = { this@DeviceListPage.drawerOpen },
                currentPage = { "DeviceList" },
                onClose = { this@DeviceListPage.drawerOpen = false },
                onNavigate = { this@DeviceListPage.navigateTo(it) }
            )
        }
    }

    private fun refreshData() {
        lifecycleScope.launch {
            runCatching {
                val db = DatabaseFactory.getDatabase()
                val latestWifiRecords = WifiScanDao(db).getAllRecords().sortedByDescending { it.signalStrength }
                val latestBluetoothRecords = BluetoothScanDao(db).getAllRecords().sortedByDescending { it.rssi }
                if (wifiRecords != latestWifiRecords) {
                    wifiRecords.clear()
                    wifiRecords.addAll(latestWifiRecords)
                }
                if (bluetoothRecords != latestBluetoothRecords) {
                    bluetoothRecords.clear()
                    bluetoothRecords.addAll(latestBluetoothRecords)
                }
                wifiDeviceCount = latestWifiRecords.size
                bluetoothDeviceCount = latestBluetoothRecords.size
                wifiSeenTotal = latestWifiRecords.sumOf { it.count }
                bluetoothSeenTotal = latestBluetoothRecords.sumOf { it.count }
            }.onFailure { it.printStackTrace() }
        }
    }

    private fun openDeviceDetail(deviceType: String, key: String) {
        val pageData = JSONObject().apply {
            put("deviceType", deviceType)
            put("deviceKey", key)
        }
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage("DeviceDetail", pageData)
    }

    private fun navigateTo(pageName: String) {
        drawerOpen = false
        if (pageName == "DeviceList") return
        if (pageName == "Scanner") {
            closePage()
            return
        }
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(pageName = pageName)
        closePage()
    }

    private fun closePage() {
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
    }
}
