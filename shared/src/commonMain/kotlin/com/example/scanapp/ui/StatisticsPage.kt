package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.LocationDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.coroutines.delay
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.View

@Page("Statistics")
class StatisticsPage : Pager() {

    private var totalWifi by observable(0L)
    private var totalBluetooth by observable(0L)
    private var totalLocations by observable(0L)
    private var drawerOpen by observable(false)
    private var topWifi by observable(emptyList<WifiScanRecord>())
    private var topBluetooth by observable(emptyList<BluetoothScanRecord>())

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
        val root = this
        View {
            attr {
                size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                backgroundColor(MdcTheme.Colors.background)
                flexDirection(FlexDirection.COLUMN)
                padding(MdcTheme.Spacing.md)
            }

            MdcMenuTopBar("Statistics") { this@StatisticsPage.drawerOpen = true }

            MdcCardRow {
                MdcStatBadge("WiFi", "${this@StatisticsPage.totalWifi}", MdcTheme.Colors.wifi)
                MdcStatBadge("Bluetooth", "${this@StatisticsPage.totalBluetooth}", MdcTheme.Colors.bluetooth)
                MdcStatBadge("Locations", "${this@StatisticsPage.totalLocations}", MdcTheme.Colors.warning)
            }

            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }
                MdcSectionHeader("Top WiFi")
                this@StatisticsPage.topWifi.forEachIndexed { index, record ->
                    this@StatisticsPage.run { root.MdcRankingRow(index, record.ssid, record.count, MdcTheme.Colors.wifi) }
                }
                if (this@StatisticsPage.topWifi.isEmpty()) MdcBodyText("No WiFi data", MdcTheme.Colors.onSurfaceVariant)

                MdcSectionHeader("Top Bluetooth")
                this@StatisticsPage.topBluetooth.forEachIndexed { index, record ->
                    this@StatisticsPage.run { root.MdcRankingRow(index, record.name, record.count, MdcTheme.Colors.bluetooth) }
                }
                if (this@StatisticsPage.topBluetooth.isEmpty()) MdcBodyText("No Bluetooth data", MdcTheme.Colors.onSurfaceVariant)
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

    private fun refreshData() {
        lifecycleScope.launch {
            runCatching {
                val db = DatabaseFactory.getDatabase()
                val wifiDao = WifiScanDao(db)
                val bluetoothDao = BluetoothScanDao(db)
                val locationDao = LocationDao(db)

                totalWifi = wifiDao.getCount()
                totalBluetooth = bluetoothDao.getCount()
                totalLocations = locationDao.getCount()
                topWifi = wifiDao.getAllRecords().sortedByDescending { it.count }.take(5)
                topBluetooth = bluetoothDao.getAllRecords().sortedByDescending { it.count }.take(5)
            }.onFailure { it.printStackTrace() }
        }
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
