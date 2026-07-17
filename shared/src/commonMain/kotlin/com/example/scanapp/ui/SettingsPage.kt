package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.CellScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.LocationDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.logging.CrashLogger
import com.example.scanapp.service.ExportServiceImpl
import com.example.scanapp.service.PlatformExportController
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.View

@Page("Settings")
class SettingsPage : Pager() {

    private var exportResult by observable("")
    private var totalWifi by observable(0L)
    private var totalBluetooth by observable(0L)
    private var totalCell by observable(0L)
    private var totalLocations by observable(0L)
    private var drawerOpen by observable(false)

    override fun created() {
        super.created()
        loadSummary()
    }

    override fun pageDidAppear() {
        super.pageDidAppear()
        loadSummary()
    }

    override fun body(): ViewContainer<*, *>.() -> Unit = {
        View {
            attr {
                size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                backgroundColor(MdcTheme.Colors.background)
                flexDirection(FlexDirection.COLUMN)
                padding(MdcTheme.Spacing.md)
            }

            MdcMenuTopBar("Settings") { this@SettingsPage.drawerOpen = true }

            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }

                MdcSectionHeader("Storage")
                MdcCardRow {
                    MdcStatBadge("WiFi", { "${this@SettingsPage.totalWifi}" }, MdcTheme.Colors.wifi)
                    MdcStatBadge("Bluetooth", { "${this@SettingsPage.totalBluetooth}" }, MdcTheme.Colors.bluetooth)
                    MdcStatBadge("Cell", { "${this@SettingsPage.totalCell}" }, MdcTheme.Colors.cell)
                    MdcStatBadge("Locations", { "${this@SettingsPage.totalLocations}" }, MdcTheme.Colors.warning)
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

                vif({ this@SettingsPage.exportResult.isNotEmpty() }) {
                    MdcBodyText({ this@SettingsPage.exportResult }, MdcTheme.Colors.secondary)
                }

                MdcSectionHeader("Data Management")
                MdcErrorButton("Clear All Data") { this@SettingsPage.clearAllData() }

                MdcSectionHeader("About")
                MdcBodyText("ScanApp v1.0")
                MdcCaption("WiFi / Bluetooth / Cell scanner built with Kuikly KMP")
            }

            MdcNavigationDrawerHost(
                isOpen = { this@SettingsPage.drawerOpen },
                currentPage = { "Settings" },
                pageHeight = this@SettingsPage.pagerData.pageViewHeight,
                onClose = { this@SettingsPage.drawerOpen = false },
                onNavigate = { this@SettingsPage.navigateTo(it) }
            )
        }
    }

    private fun exportData(format: String) {
        safeLaunch("Settings.export") {
            runCatching {
                val db = DatabaseFactory.getDatabase()
                val wifiRecords = WifiScanDao(db).getAllRecords()
                val bluetoothRecords = BluetoothScanDao(db).getAllRecords()
                val cellRecords = CellScanDao(db).getAllRecords()
                val locationRecords = LocationDao(db).getAllRecords()
                val exporter = ExportServiceImpl()
                val result = if (format == "csv") {
                    exporter.exportToCsv(wifiRecords, bluetoothRecords, locationRecords, cellRecords)
                } else {
                    exporter.exportToJson(wifiRecords, bluetoothRecords, locationRecords, cellRecords)
                }
                val exportFileResult = PlatformExportController.exportAndShareFile(
                    fileName = "scanapp-export.$format",
                    content = result,
                    mimeType = if (format == "csv") "text/csv" else "application/json"
                )
                exportResult = if (exportFileResult.success) {
                    "${format.uppercase()} export ready: ${exportFileResult.filePath}"
                } else {
                    "Export failed: ${exportFileResult.message}"
                }
            }.onFailure {
                exportResult = "Export failed: ${it.message}"
                CrashLogger.log("Settings.export", it)
            }
        }
    }

    private fun clearAllData() {
        safeLaunch("Settings.clear") {
            runCatching {
                val db = DatabaseFactory.getDatabase()
                WifiScanDao(db).deleteAll()
                BluetoothScanDao(db).deleteAll()
                CellScanDao(db).deleteAll()
                LocationDao(db).deleteAll()
                totalWifi = 0L
                totalBluetooth = 0L
                totalCell = 0L
                totalLocations = 0L
                exportResult = "All data cleared"
            }.onFailure {
                exportResult = "Clear failed: ${it.message}"
                CrashLogger.log("Settings.clear", it)
            }
        }
    }

    private fun loadSummary() {
        safeLaunch("Settings.loadSummary") {
            runCatching {
                val db = DatabaseFactory.getDatabase()
                totalWifi = WifiScanDao(db).getCount()
                totalBluetooth = BluetoothScanDao(db).getCount()
                totalCell = CellScanDao(db).getCount()
                totalLocations = LocationDao(db).getCount()
            }.onFailure { CrashLogger.log("Settings.loadSummary", it) }
        }
    }

    private fun navigateTo(pageName: String) {
        drawerOpen = false
        if (pageName == "Settings") return
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
