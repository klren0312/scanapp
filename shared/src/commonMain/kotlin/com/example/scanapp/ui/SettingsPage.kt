package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.LocationDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.service.ExportServiceImpl
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.View

@Page("Settings")
class SettingsPage : Pager() {

    private var exportResult = ""

    override fun body(): ViewContainer<*, *>.() -> Unit = {
        val root = this
        View {
            attr {
                size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                backgroundColor(Color("#F5F5F5"))
                flexDirection(FlexDirection.COLUMN)
                padding(16f)
            }

            TitleText("Settings")

            Scroller {
                attr {
                    flex(1f)
                    marginTop(12f)
                }

                InfoText("Data Export", Color("#333333"))
                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                        marginTop(8f)
                    }
                    ActionButton("Export CSV", Color("#2196F3")) { this@SettingsPage.exportData("csv") }
                    ActionButton("Export JSON", Color("#4CAF50")) { this@SettingsPage.exportData("json") }
                }

                if (this@SettingsPage.exportResult.isNotEmpty()) {
                    InfoText(this@SettingsPage.exportResult, Color("#4CAF50"))
                }

                InfoText("Data Management", Color("#333333"))
                ActionButton("Clear All Data", Color("#F44336")) { this@SettingsPage.clearAllData() }

                InfoText("About", Color("#333333"))
                InfoText("ScanApp v1.0")
                InfoText("WiFi / Bluetooth scanner built with Kuikly KMP")
            }
        }
    }

    private fun exportData(format: String) {
        lifecycleScope.launch {
            runCatching {
                val db = DatabaseFactory.getDatabase()
                val wifiRecords = WifiScanDao(db).getAllRecords()
                val bluetoothRecords = BluetoothScanDao(db).getAllRecords()
                val locationRecords = LocationDao(db).getAllRecords()
                val exporter = ExportServiceImpl()
                val result = if (format == "csv") {
                    exporter.exportToCsv(wifiRecords, bluetoothRecords, locationRecords)
                } else {
                    exporter.exportToJson(wifiRecords, bluetoothRecords, locationRecords)
                }
                exportResult = "${format.uppercase()} export succeeded (${result.length} chars)"
            }.onFailure {
                exportResult = "Export failed: ${it.message}"
                it.printStackTrace()
            }
        }
    }

    private fun clearAllData() {
        lifecycleScope.launch {
            runCatching {
                val db = DatabaseFactory.getDatabase()
                WifiScanDao(db).deleteAll()
                BluetoothScanDao(db).deleteAll()
                LocationDao(db).deleteAll()
                exportResult = "All data cleared"
            }.onFailure {
                exportResult = "Clear failed: ${it.message}"
                it.printStackTrace()
            }
        }
    }
}
