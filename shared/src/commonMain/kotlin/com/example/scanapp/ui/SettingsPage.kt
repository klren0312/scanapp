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
                backgroundColor(MdcTheme.Colors.background)
                flexDirection(FlexDirection.COLUMN)
                padding(MdcTheme.Spacing.md)
            }

            MdcTitle("Settings")

            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }

                MdcSectionHeader("Data Export")
                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        justifyContent(FlexJustifyContent.SPACE_EVENLY)
                        marginTop(MdcTheme.Spacing.sm)
                    }
                    MdcFilledButton("Export CSV") { this@SettingsPage.exportData("csv") }
                    MdcFilledButton("Export JSON") { this@SettingsPage.exportData("json") }
                }

                if (this@SettingsPage.exportResult.isNotEmpty()) {
                    MdcBodyText(this@SettingsPage.exportResult, MdcTheme.Colors.secondary)
                }

                MdcSectionHeader("Data Management")
                MdcErrorButton("Clear All Data") { this@SettingsPage.clearAllData() }

                MdcSectionHeader("About")
                MdcBodyText("ScanApp v1.0")
                MdcCaption("WiFi / Bluetooth scanner built with Kuikly KMP")
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
