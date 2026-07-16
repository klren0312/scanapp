package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.CellScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.LocationDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.logging.CrashLogger
import com.example.scanapp.util.diffUpdate
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.View

private data class RankingItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val count: Int
)

@Page("Statistics")
class StatisticsPage : Pager() {

    private var totalWifi by observable(0L)
    private var totalBluetooth by observable(0L)
    private var totalCell by observable(0L)
    private var totalLocations by observable(0L)
    private var drawerOpen by observable(false)
    private var topWifi by observableList<RankingItem>()
    private var topBluetooth by observableList<RankingItem>()
    private var topCell by observableList<RankingItem>()
    private var isPageActive = true

    override fun created() {
        super.created()
        safeLaunch("Statistics.created") { refreshData() }
    }

    override fun pageWillDestroy() {
        isPageActive = false
        super.pageWillDestroy()
    }

    override fun pageDidAppear() {
        super.pageDidAppear()
        refresh()
    }

    private fun refresh() {
        safeLaunch("Statistics.refresh") { refreshData() }
    }

    override fun body(): ViewContainer<*, *>.() -> Unit = {
        View {
            attr {
                size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                backgroundColor(MdcTheme.Colors.background)
                flexDirection(FlexDirection.COLUMN)
                padding(MdcTheme.Spacing.md)
            }

            MdcMenuTopBar("Statistics") { this@StatisticsPage.drawerOpen = true }

            MdcCardRow {
                MdcStatBadge("WiFi", { "${this@StatisticsPage.totalWifi}" }, MdcTheme.Colors.wifi)
                MdcStatBadge("Bluetooth", { "${this@StatisticsPage.totalBluetooth}" }, MdcTheme.Colors.bluetooth)
                MdcStatBadge("Cell", { "${this@StatisticsPage.totalCell}" }, MdcTheme.Colors.cell)
                MdcStatBadge("Locations", { "${this@StatisticsPage.totalLocations}" }, MdcTheme.Colors.warning)
            }

            MdcOutlinedButton("Refresh") { this@StatisticsPage.refresh() }

            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }
                MdcSectionHeader("Top WiFi")
                vforIndex({ this@StatisticsPage.topWifi }) { record, index, _ ->
                    val itemContainer = this
                    this@StatisticsPage.run {
                        itemContainer.MdcRankingRow(index, record, MdcTheme.Colors.wifi)
                    }
                }
                vif({ this@StatisticsPage.totalWifi == 0L }) {
                    MdcBodyText("No WiFi data", MdcTheme.Colors.onSurfaceVariant)
                }

                MdcSectionHeader("Top Bluetooth")
                vforIndex({ this@StatisticsPage.topBluetooth }) { record, index, _ ->
                    val itemContainer = this
                    this@StatisticsPage.run {
                        itemContainer.MdcRankingRow(index, record, MdcTheme.Colors.bluetooth)
                    }
                }
                vif({ this@StatisticsPage.totalBluetooth == 0L }) {
                    MdcBodyText("No Bluetooth data", MdcTheme.Colors.onSurfaceVariant)
                }

                MdcSectionHeader("Top Cell")
                vforIndex({ this@StatisticsPage.topCell }) { record, index, _ ->
                    val itemContainer = this
                    this@StatisticsPage.run {
                        itemContainer.MdcRankingRow(index, record, MdcTheme.Colors.cell)
                    }
                }
                vif({ this@StatisticsPage.totalCell == 0L }) {
                    MdcBodyText("No Cell data", MdcTheme.Colors.onSurfaceVariant)
                }
            }

            MdcNavigationDrawerHost(
                isOpen = { this@StatisticsPage.drawerOpen },
                currentPage = { "Statistics" },
                pageHeight = this@StatisticsPage.pagerData.pageViewHeight,
                onClose = { this@StatisticsPage.drawerOpen = false },
                onNavigate = { this@StatisticsPage.navigateTo(it) }
            )
        }
    }

    private fun ViewContainer<*, *>.MdcRankingRow(index: Int, item: RankingItem, color: Color) {
        MdcListItem(
            title = "${index + 1}. ${item.title}",
            subtitle = item.subtitle,
            trailing = "${item.count} times",
            trailingColor = color
        )
    }

    private suspend fun refreshData() {
        if (!isPageActive) return
        runCatching {
            val db = DatabaseFactory.getDatabase()
            val wifiDao = WifiScanDao(db)
            val bluetoothDao = BluetoothScanDao(db)
            val cellDao = CellScanDao(db)
            val locationDao = LocationDao(db)

            totalWifi = wifiDao.getCount()
            totalBluetooth = bluetoothDao.getCount()
            totalCell = cellDao.getCount()
            totalLocations = locationDao.getCount()
            val latestTopWifi = wifiDao.getAllRecords()
                .sortedWith(
                    compareByDescending<com.example.scanapp.models.WifiScanRecord> { it.count }
                        .thenByDescending { it.timestamp }
                )
                .take(5)
                .map {
                    RankingItem(
                        key = it.bssid,
                        title = it.ssid.ifEmpty { "Unknown WiFi" },
                        subtitle = it.bssid,
                        count = it.count
                    )
                }
            val latestTopBluetooth = bluetoothDao.getAllRecords()
                .sortedWith(
                    compareByDescending<com.example.scanapp.models.BluetoothScanRecord> { it.count }
                        .thenByDescending { it.timestamp }
                )
                .take(5)
                .map {
                    RankingItem(
                        key = it.address,
                        title = it.name.ifEmpty { "Unknown Bluetooth" },
                        subtitle = it.address,
                        count = it.count
                    )
                }
            val latestTopCell = cellDao.getAllRecords()
                .sortedWith(
                    compareByDescending<com.example.scanapp.models.CellScanRecord> { it.count }
                        .thenByDescending { it.timestamp }
                )
                .take(5)
                .map {
                    val name = if (it.operator.isNotEmpty() && it.operator != "Unknown") {
                        it.operator
                    } else {
                        "${it.networkType} Cell"
                    }
                    RankingItem(
                        key = it.cellKey,
                        title = name,
                        subtitle = "${it.networkType} | ${it.cellKey}",
                        count = it.count
                    )
                }
            if (!isPageActive) return
            topWifi.diffUpdate(latestTopWifi)
            topBluetooth.diffUpdate(latestTopBluetooth)
            topCell.diffUpdate(latestTopCell)
        }.onFailure { CrashLogger.log("Statistics.refreshData", it) }
    }

    private fun navigateTo(pageName: String) {
        drawerOpen = false
        if (pageName == "Statistics") return
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
