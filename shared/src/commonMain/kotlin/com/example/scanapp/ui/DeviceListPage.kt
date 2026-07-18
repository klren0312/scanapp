package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.CellScanDao
import com.example.scanapp.service.cellReadinessHint
import com.example.scanapp.service.requestCellScanPermission
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.CellScanRecord
import com.example.scanapp.models.WifiScanRecord
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.module.CalendarModule
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.example.scanapp.util.diffUpdate

private data class DeviceItem(
    val type: String,
    val title: String,
    val identity: String,
    val primaryMetric: String,
    val secondaryMetric: String,
    val count: Int,
    val key: String,
    val timestamp: Long
)

@Page("DeviceList")
class DeviceListPage : Pager() {

    private val pageSize = 30

    private var wifiDeviceCount by observable(0)
    private var bluetoothDeviceCount by observable(0)
    private var cellDeviceCount by observable(0)
    private var cellHint by observable("")
    private var wifiSeenTotal by observable(0)
    private var bluetoothSeenTotal by observable(0)
    private var cellSeenTotal by observable(0)
    private var deviceFilter by observable("all")
    private var drawerOpen by observable(false)
    private var isLoadingMore by observable(false)
    private var displayRecords by observableList<DeviceItem>()

    private var isPageActive = true
    private var hasAppeared = false
    private var isInitialLoading = false
    private var loadToken = 0
    private val loadedItems = mutableListOf<DeviceItem>()
    private var loadedWifiCount = 0
    private var loadedBluetoothCount = 0
    private var loadedCellCount = 0
    private var wifiTotal = 0
    private var bluetoothTotal = 0
    private var cellTotal = 0

    override fun created() {
        super.created()
        requestInitialLoad("DeviceList.created")
    }

    override fun pageWillDestroy() {
        isPageActive = false
        loadToken++
        super.pageWillDestroy()
    }

    override fun pageDidAppear() {
        super.pageDidAppear()
        if (hasAppeared) {
            refresh()
        } else {
            hasAppeared = true
        }
    }

    override fun body(): ViewContainer<*, *>.() -> Unit = {
        safe("DeviceList.body") {
            View {
                attr {
                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                    backgroundColor(MdcTheme.Colors.background)
                    flexDirection(FlexDirection.COLUMN)
                    padding(MdcTheme.Spacing.md)
                }

                MdcMenuTopBar("Devices") { this@DeviceListPage.drawerOpen = true }

                MdcCardRow {
                    MdcStatBadge("WiFi Devices", { safeValue("DeviceList.stat") { "${this@DeviceListPage.wifiDeviceCount}" } }, MdcTheme.Colors.wifi)
                    MdcStatBadge("Bluetooth Devices", { safeValue("DeviceList.stat") { "${this@DeviceListPage.bluetoothDeviceCount}" } }, MdcTheme.Colors.bluetooth)
                    MdcStatBadge("Cell Devices", { safeValue("DeviceList.stat") { "${this@DeviceListPage.cellDeviceCount}" } }, MdcTheme.Colors.cell)
                }

                MdcCardRow(elevation = MdcTheme.Elevation.level0) {
                    MdcStatBadge("WiFi Seen", { safeValue("DeviceList.stat") { "${this@DeviceListPage.wifiSeenTotal}" } }, MdcTheme.Colors.wifi)
                    MdcStatBadge("Bluetooth Seen", { safeValue("DeviceList.stat") { "${this@DeviceListPage.bluetoothSeenTotal}" } }, MdcTheme.Colors.bluetooth)
                    MdcStatBadge("Cell Seen", { safeValue("DeviceList.stat") { "${this@DeviceListPage.cellSeenTotal}" } }, MdcTheme.Colors.cell)
                }

                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        marginTop(MdcTheme.Spacing.sm)
                    }
                    MdcTab("All", { safeBool("DeviceList.tab") { this@DeviceListPage.deviceFilter == "all" } }) { this@DeviceListPage.setFilter("all") }
                    MdcTab("WiFi", { safeBool("DeviceList.tab") { this@DeviceListPage.deviceFilter == "wifi" } }) { this@DeviceListPage.setFilter("wifi") }
                    MdcTab("Bluetooth", { safeBool("DeviceList.tab") { this@DeviceListPage.deviceFilter == "bluetooth" } }) { this@DeviceListPage.setFilter("bluetooth") }
                    MdcTab("Cell", { safeBool("DeviceList.tab") { this@DeviceListPage.deviceFilter == "cell" } }) { this@DeviceListPage.setFilter("cell") }
                }

                MdcOutlinedButton("Refresh") { this@DeviceListPage.refresh() }

                List {
                    attr {
                        flex(1f)
                        marginTop(MdcTheme.Spacing.sm)
                    }
                    event {
                        scrollEnd {
                            val reachBottom = it.offsetY + it.viewHeight >= it.contentHeight - 200f
                            if (reachBottom) {
                                this@DeviceListPage.requestLoadMore()
                            }
                        }
                    }
                    vforLazy({ this@DeviceListPage.displayRecords }) { item, _, _ ->
                        safe("DeviceList.vforLazy") {
                            MdcDeviceCard(
                                title = item.title,
                                identity = item.identity,
                                primaryMetric = item.primaryMetric,
                                secondaryMetric = item.secondaryMetric,
                                count = item.count,
                                scanTime = this@DeviceListPage.formatScanTime(item.timestamp),
                                color = when (item.type) {
                                    "wifi" -> MdcTheme.Colors.wifi
                                    "cell" -> MdcTheme.Colors.cell
                                    else -> MdcTheme.Colors.bluetooth
                                }
                            ) {
                                this@DeviceListPage.openDeviceDetail(item.type, item.key)
                            }
                        }
                    }
                    vif({ this@DeviceListPage.displayRecords.isEmpty() }) {
                        if (this@DeviceListPage.deviceFilter == "cell" && this@DeviceListPage.cellHint.isNotEmpty()) {
                            View {
                                attr {
                                    marginTop(MdcTheme.Spacing.sm)
                                    padding(MdcTheme.Spacing.md)
                                    backgroundColor(MdcTheme.Colors.surfaceVariant)
                                    borderRadius(12f)
                                }
                                MdcBodyText(this@DeviceListPage.cellHint, MdcTheme.Colors.warning)
                                vif({ this@DeviceListPage.cellHint.contains("location permission") }) {
                                    View {
                                        attr {
                                            marginTop(MdcTheme.Spacing.sm)
                                            padding(top = 8f, bottom = 8f, left = 12f, right = 12f)
                                            backgroundColor(MdcTheme.Colors.primary)
                                            borderRadius(16f)
                                            alignItems(FlexAlign.CENTER)
                                            justifyContent(FlexJustifyContent.CENTER)
                                        }
                                        event {
                                            click { this@DeviceListPage.requestCellPermission() }
                                        }
                                        Text {
                                            attr {
                                                text("Grant location permission")
                                                fontSize(MdcTheme.Typography.labelLarge)
                                                fontWeightSemiBold()
                                                color(MdcTheme.Colors.onPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            MdcBodyText("No devices", MdcTheme.Colors.onSurfaceVariant)
                        }
                    }
                    vif({ this@DeviceListPage.isLoadingMore }) {
                        MdcBodyText("Loading more...", MdcTheme.Colors.onSurfaceVariant)
                    }
                }

                MdcNavigationDrawerHost(
                    isOpen = { this@DeviceListPage.drawerOpen },
                    currentPage = { "DeviceList" },
                    pageHeight = this@DeviceListPage.pagerData.pageViewHeight,
                    onClose = { this@DeviceListPage.drawerOpen = false },
                    onNavigate = { this@DeviceListPage.navigateTo(it) }
                )
            }
        }
    }

    private fun refresh() {
        isLoadingMore = false
        requestInitialLoad("DeviceList.refresh")
    }

    private fun requestLoadMore() {
        if (isInitialLoading || isLoadingMore || !hasMore()) return
        isLoadingMore = true
        requestLoad("DeviceList.loadMore") { token ->
            try {
                loadMore(token)
            } finally {
                if (token == loadToken) isLoadingMore = false
            }
        }
    }

    private fun requestInitialLoad(tag: String) {
        isInitialLoading = true
        requestLoad(tag) { token ->
            try {
                loadInitial(token)
            } finally {
                if (token == loadToken) isInitialLoading = false
            }
        }
    }

    private fun requestLoad(tag: String, block: suspend (Int) -> Unit) {
        loadToken++
        val token = loadToken
        safeLaunch(tag) { block(token) }
    }

    private fun hasMore(): Boolean = loadedWifiCount < wifiTotal || loadedBluetoothCount < bluetoothTotal || loadedCellCount < cellTotal

    private suspend fun loadInitial(token: Int) {
        if (!isPageActive) return
        val db = DatabaseFactory.getDatabase()
        val wifiDao = WifiScanDao(db)
        val bluetoothDao = BluetoothScanDao(db)
        val cellDao = CellScanDao(db)

        val nextWifiTotal = wifiDao.getCount().toInt()
        val nextBluetoothTotal = bluetoothDao.getCount().toInt()
        val nextCellTotal = cellDao.getCount().toInt()
        val nextWifiSeenTotal = wifiDao.getSeenTotal().toInt()
        val nextBluetoothSeenTotal = bluetoothDao.getSeenTotal().toInt()
        val nextCellSeenTotal = cellDao.getSeenTotal().toInt()

        val wifiPage = wifiDao.getRecordsPaginated(pageSize, 0)
        val bluetoothPage = bluetoothDao.getRecordsPaginated(pageSize, 0)
        val cellPage = cellDao.getRecordsPaginated(pageSize, 0)

        if (!isPageActive || token != loadToken) return
        wifiTotal = nextWifiTotal
        bluetoothTotal = nextBluetoothTotal
        cellTotal = nextCellTotal
        wifiDeviceCount = nextWifiTotal
        bluetoothDeviceCount = nextBluetoothTotal
        cellDeviceCount = nextCellTotal
        cellHint = cellReadinessHint(nextCellTotal.toLong())
        wifiSeenTotal = nextWifiSeenTotal
        bluetoothSeenTotal = nextBluetoothSeenTotal
        cellSeenTotal = nextCellSeenTotal
        loadedWifiCount = wifiPage.size
        loadedBluetoothCount = bluetoothPage.size
        loadedCellCount = cellPage.size

        loadedItems.clear()
        loadedItems.addAll(wifiPage.map { it.toDeviceItem() })
        loadedItems.addAll(bluetoothPage.map { it.toDeviceItem() })
        loadedItems.addAll(cellPage.map { it.toDeviceItem() })

        rebuildDisplay()
    }

    private suspend fun loadMore(token: Int) {
        if (!isPageActive || !hasMore()) return
        val wifiOffset = loadedWifiCount
        val bluetoothOffset = loadedBluetoothCount
        val cellOffset = loadedCellCount
        val wifiLimit = wifiTotal
        val bluetoothLimit = bluetoothTotal
        val cellLimit = cellTotal
        val db = DatabaseFactory.getDatabase()
        val wifiDao = WifiScanDao(db)
        val bluetoothDao = BluetoothScanDao(db)
        val cellDao = CellScanDao(db)

        val wifiPage = if (wifiOffset < wifiLimit) {
            wifiDao.getRecordsPaginated(pageSize, wifiOffset)
        } else {
            emptyList()
        }
        val bluetoothPage = if (bluetoothOffset < bluetoothLimit) {
            bluetoothDao.getRecordsPaginated(pageSize, bluetoothOffset)
        } else {
            emptyList()
        }
        val cellPage = if (cellOffset < cellLimit) {
            cellDao.getRecordsPaginated(pageSize, cellOffset)
        } else {
            emptyList()
        }

        if (!isPageActive || token != loadToken) return
        loadedWifiCount += wifiPage.size
        loadedBluetoothCount += bluetoothPage.size
        loadedCellCount += cellPage.size
        cellHint = cellReadinessHint(cellTotal.toLong())
        loadedItems.addAll(wifiPage.map { it.toDeviceItem() })
        loadedItems.addAll(bluetoothPage.map { it.toDeviceItem() })
        loadedItems.addAll(cellPage.map { it.toDeviceItem() })
        rebuildDisplay()
    }


    private fun requestCellPermission() {
        requestCellScanPermission()
    }
    private fun setFilter(type: String) {
        if (deviceFilter == type) return
        deviceFilter = type
        rebuildDisplay()
    }

    private fun rebuildDisplay() {
        val filtered = loadedItems.filter {
            deviceFilter == "all" || it.type == deviceFilter
        }
        val sorted = filtered.sortedByDescending { it.timestamp }
        displayRecords.diffUpdate(sorted)
    }

    private fun WifiScanRecord.toDeviceItem() = DeviceItem(
        type = "wifi",
        title = ssid.ifEmpty { "Unknown" },
        identity = bssid,
        primaryMetric = "$signalStrength dBm",
        secondaryMetric = "$frequency MHz",
        count = count,
        key = bssid,
        timestamp = timestamp
    )

    private fun BluetoothScanRecord.toDeviceItem() = DeviceItem(
        type = "bluetooth",
        title = name.ifEmpty { "Unknown" },
        identity = address,
        primaryMetric = "$rssi dBm",
        secondaryMetric = deviceType,
        count = count,
        key = address,
        timestamp = timestamp
    )

    private fun CellScanRecord.toDeviceItem() = DeviceItem(
        type = "cell",
        title = if (operator.isNotEmpty() && operator != "Unknown") operator else "$networkType Cell",
        identity = cellKey,
        primaryMetric = "$signalStrength dBm",
        secondaryMetric = "$networkType 路 MCC $mcc MNC $mnc",
        count = count,
        key = cellKey,
        timestamp = timestamp
    )

    private fun formatScanTime(timestamp: Long): String {
        return runCatching {
            acquireModule<CalendarModule>(CalendarModule.MODULE_NAME)
                .formatTime(timestamp, "yyyy-MM-dd HH:mm:ss")
        }.getOrElse { timestamp.toString() }
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
