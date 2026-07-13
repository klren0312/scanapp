package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewContainer
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

private data class DeviceItem(
    val type: String,
    val title: String,
    val identity: String,
    val primaryMetric: String,
    val secondaryMetric: String,
    val count: Int,
    val key: String
)

@Page("DeviceList")
class DeviceListPage : Pager() {

    private var wifiDeviceCount by observable(0)
    private var bluetoothDeviceCount by observable(0)
    private var wifiSeenTotal by observable(0)
    private var bluetoothSeenTotal by observable(0)
    private var deviceFilter by observable("all")
    private var drawerOpen by observable(false)
    private var wifiRecords by observableList<WifiScanRecord>()
    private var bluetoothRecords by observableList<BluetoothScanRecord>()
    private var displayRecords by observableList<DeviceItem>()
    private var isPageActive = true

    override fun created() {
        super.created()
        lifecycleScope.launch { refreshData() }
    }

    override fun pageWillDestroy() {
        isPageActive = false
        super.pageWillDestroy()
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

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    marginTop(MdcTheme.Spacing.sm)
                }
                MdcTab("All", { this@DeviceListPage.deviceFilter == "all" }) { this@DeviceListPage.setFilter("all") }
                MdcTab("WiFi", { this@DeviceListPage.deviceFilter == "wifi" }) { this@DeviceListPage.setFilter("wifi") }
                MdcTab("Bluetooth", { this@DeviceListPage.deviceFilter == "bluetooth" }) { this@DeviceListPage.setFilter("bluetooth") }
            }

            MdcOutlinedButton("Refresh") { this@DeviceListPage.refresh() }

            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }
                vfor({ this@DeviceListPage.displayRecords }) { item ->
                    MdcDeviceCard(
                        title = item.title,
                        identity = item.identity,
                        primaryMetric = item.primaryMetric,
                        secondaryMetric = item.secondaryMetric,
                        count = item.count,
                        color = if (item.type == "wifi") MdcTheme.Colors.wifi else MdcTheme.Colors.bluetooth
                    ) {
                        this@DeviceListPage.openDeviceDetail(item.type, item.key)
                    }
                }
                vif({ this@DeviceListPage.displayRecords.isEmpty() }) {
                    MdcBodyText("No devices", MdcTheme.Colors.onSurfaceVariant)
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

    private fun refresh() {
        lifecycleScope.launch { refreshData() }
    }

    private suspend fun refreshData() {
        if (!isPageActive) return
        runCatching {
            val db = DatabaseFactory.getDatabase()
            val latestWifiRecords = WifiScanDao(db).getAllRecords().sortedByDescending { it.signalStrength }
            val latestBluetoothRecords = BluetoothScanDao(db).getAllRecords().sortedByDescending { it.rssi }
            if (!isPageActive) return
            wifiRecords.clear()
            wifiRecords.addAll(latestWifiRecords)
            bluetoothRecords.clear()
            bluetoothRecords.addAll(latestBluetoothRecords)
            wifiDeviceCount = latestWifiRecords.size
            bluetoothDeviceCount = latestBluetoothRecords.size
            wifiSeenTotal = latestWifiRecords.sumOf { it.count }
            bluetoothSeenTotal = latestBluetoothRecords.sumOf { it.count }
            rebuildDisplay()
        }.onFailure { it.printStackTrace() }
    }

    private fun setFilter(type: String) {
        if (deviceFilter == type) return
        deviceFilter = type
        rebuildDisplay()
    }

    private fun rebuildDisplay() {
        val items = mutableListOf<DeviceItem>()
        if (deviceFilter == "all" || deviceFilter == "wifi") {
            wifiRecords.forEach {
                items.add(
                    DeviceItem(
                        type = "wifi",
                        title = it.ssid.ifEmpty { "Unknown" },
                        identity = it.bssid,
                        primaryMetric = "${it.signalStrength} dBm",
                        secondaryMetric = "${it.frequency} MHz",
                        count = it.count,
                        key = it.bssid
                    )
                )
            }
        }
        if (deviceFilter == "all" || deviceFilter == "bluetooth") {
            bluetoothRecords.forEach {
                items.add(
                    DeviceItem(
                        type = "bluetooth",
                        title = it.name.ifEmpty { "Unknown" },
                        identity = it.address,
                        primaryMetric = "${it.rssi} dBm",
                        secondaryMetric = it.deviceType,
                        count = it.count,
                        key = it.address
                    )
                )
            }
        }
        displayRecords.clear()
        displayRecords.addAll(items)
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
