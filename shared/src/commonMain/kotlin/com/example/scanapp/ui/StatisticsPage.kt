package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.LocationDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.logging.CrashLogger
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
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

@Page("Statistics")
class StatisticsPage : Pager() {

    private var totalWifi by observable(0L)
    private var totalBluetooth by observable(0L)
    private var totalLocations by observable(0L)
    private var drawerOpen by observable(false)
    private var topWifi by observableList<WifiScanRecord>()
    private var topBluetooth by observableList<BluetoothScanRecord>()
    private var isPageActive = true

    override fun created() {
        super.created()
        safeLaunch("Statistics.created") { refreshData() }
    }

    override fun pageWillDestroy() {
        isPageActive = false
        super.pageWillDestroy()
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
                        itemContainer.MdcRankingRow(index, record.ssid, record.count, MdcTheme.Colors.wifi)
                    }
                }
                vif({ this@StatisticsPage.totalWifi == 0L }) {
                    MdcBodyText("No WiFi data", MdcTheme.Colors.onSurfaceVariant)
                }

                MdcSectionHeader("Top Bluetooth")
                vforIndex({ this@StatisticsPage.topBluetooth }) { record, index, _ ->
                    val itemContainer = this
                    this@StatisticsPage.run {
                        itemContainer.MdcRankingRow(index, record.name, record.count, MdcTheme.Colors.bluetooth)
                    }
                }
                vif({ this@StatisticsPage.totalBluetooth == 0L }) {
                    MdcBodyText("No Bluetooth data", MdcTheme.Colors.onSurfaceVariant)
                }
            }

            MdcNavigationDrawerHost(
                isOpen = { this@StatisticsPage.drawerOpen },
                currentPage = { "Statistics" },
                onClose = { this@StatisticsPage.drawerOpen = false },
                onNavigate = { this@StatisticsPage.navigateTo(it) }
            )
        }
    }

    private fun ViewContainer<*, *>.MdcRankingRow(index: Int, name: String, count: Int, color: Color) {
        MdcListItem(
            title = "${index + 1}. ${name.ifEmpty { "Unknown" }}",
            trailing = "$count times",
            trailingColor = color
        )
    }

    private suspend fun refreshData() {
        if (!isPageActive) return
        runCatching {
            val db = DatabaseFactory.getDatabase()
            val wifiDao = WifiScanDao(db)
            val bluetoothDao = BluetoothScanDao(db)
            val locationDao = LocationDao(db)

            totalWifi = wifiDao.getCount()
            totalBluetooth = bluetoothDao.getCount()
            totalLocations = locationDao.getCount()
            val latestTopWifi = wifiDao.getAllRecords().sortedByDescending { it.count }.take(5)
            val latestTopBluetooth = bluetoothDao.getAllRecords().sortedByDescending { it.count }.take(5)
            if (!isPageActive) return
            topWifi.clear()
            topWifi.addAll(latestTopWifi)
            topBluetooth.clear()
            topBluetooth.addAll(latestTopBluetooth)
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
