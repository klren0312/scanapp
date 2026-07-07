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
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("Statistics")
class StatisticsPage : Pager() {

    private var totalWifi = 0L
    private var totalBluetooth = 0L
    private var totalLocations = 0L
    private var topWifi: List<WifiScanRecord> = emptyList()
    private var topBluetooth: List<BluetoothScanRecord> = emptyList()

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

            TitleText("Statistics")
            this@StatisticsPage.run { root.SummaryCard() }

            Scroller {
                attr {
                    flex(1f)
                    marginTop(12f)
                }
                InfoText("Top WiFi", Color("#333333"))
                this@StatisticsPage.topWifi.forEachIndexed { index, record ->
                    this@StatisticsPage.run { root.RankingRow(index, record.ssid, record.count, Color("#2196F3")) }
                }
                if (this@StatisticsPage.topWifi.isEmpty()) InfoText("No WiFi data")

                InfoText("Top Bluetooth", Color("#333333"))
                this@StatisticsPage.topBluetooth.forEachIndexed { index, record ->
                    this@StatisticsPage.run { root.RankingRow(index, record.name, record.count, Color("#4CAF50")) }
                }
                if (this@StatisticsPage.topBluetooth.isEmpty()) InfoText("No Bluetooth data")
            }
        }
    }

    private fun ViewContainer<*, *>.SummaryCard() {
        View {
            attr {
                flexDirection(FlexDirection.ROW)
                justifyContent(FlexJustifyContent.SPACE_AROUND)
                padding(16f)
                marginTop(12f)
                backgroundColor(Color.WHITE)
                borderRadius(8f)
            }
            InfoText("WiFi: ${this@StatisticsPage.totalWifi}", Color("#2196F3"))
            InfoText("Bluetooth: ${this@StatisticsPage.totalBluetooth}", Color("#4CAF50"))
            InfoText("Locations: ${this@StatisticsPage.totalLocations}", Color("#FF9800"))
        }
    }

    private fun ViewContainer<*, *>.RankingRow(index: Int, name: String, count: Int, color: Color) {
        View {
            attr {
                flexDirection(FlexDirection.ROW)
                justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                padding(10f)
                marginTop(4f)
                backgroundColor(Color.WHITE)
                borderRadius(6f)
            }
            Text {
                attr {
                    text("${index + 1}. ${name.ifEmpty { "Unknown" }}")
                    fontSize(14f)
                    color(Color("#333333"))
                }
            }
            Text {
                attr {
                    text("$count times")
                    fontSize(13f)
                    color(color)
                }
            }
        }
    }

    private fun loadData() {
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
}
