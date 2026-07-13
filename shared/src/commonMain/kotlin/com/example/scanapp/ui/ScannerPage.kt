package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.logging.CrashLogger
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import com.example.scanapp.service.PlatformScanController
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("Scanner")
class ScannerPage : Pager() {

    private var isScanning by observable(false)
    private var scanStatus by observable("")
    private var wifiCount by observable(0L)
    private var bluetoothCount by observable(0L)
    private var recentWifiText by observable("No WiFi records")
    private var recentBluetoothText by observable("No Bluetooth records")
    private var drawerOpen by observable(false)
    private var isPageActive = true

    override fun created() {
        super.created()
        refreshData()
    }

    override fun pageWillDestroy() {
        isPageActive = false
        isScanning = false
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

            MdcMenuTopBar("WiFi / Bluetooth Scanner") { this@ScannerPage.drawerOpen = true }

            MdcCardRow {
                MdcStatBadge("WiFi Networks", { "${this@ScannerPage.wifiCount}" }, MdcTheme.Colors.wifi)
                MdcStatBadge("Bluetooth", { "${this@ScannerPage.bluetoothCount}" }, MdcTheme.Colors.bluetooth)
            }

            View {
                attr {
                    marginTop(MdcTheme.Spacing.sm)
                    padding(top = 12f, bottom = 12f, left = MdcTheme.Spacing.md + 4f, right = MdcTheme.Spacing.md + 4f)
                    backgroundColor(
                        if (this@ScannerPage.isScanning) MdcTheme.Colors.error
                        else MdcTheme.Colors.primary
                    )
                    borderRadius(20f)
                    alignItems(FlexAlign.CENTER)
                    justifyContent(FlexJustifyContent.CENTER)
                    if (!this@ScannerPage.isScanning) {
                        boxShadow(MdcTheme.Elevation.level1)
                    }
                }
                event {
                    click {
                        if (this@ScannerPage.isScanning) this@ScannerPage.stopScanning()
                        else this@ScannerPage.startScanning()
                    }
                }
                Text {
                    attr {
                        text(if (this@ScannerPage.isScanning) "Stop Scanning" else "Start Scanning")
                        fontSize(MdcTheme.Typography.labelLarge)
                        fontWeightSemiBold()
                        color(
                            if (this@ScannerPage.isScanning) MdcTheme.Colors.onError
                            else MdcTheme.Colors.onPrimary
                        )
                    }
                }
            }

            Text {
                attr {
                    val message = when {
                        this@ScannerPage.scanStatus.isNotEmpty() -> this@ScannerPage.scanStatus
                        this@ScannerPage.isScanning -> "Scanning..."
                        else -> ""
                    }
                    text(message)
                    fontSize(MdcTheme.Typography.bodyLarge)
                    color(if (this@ScannerPage.isScanning) MdcTheme.Colors.warning else MdcTheme.Colors.onSurfaceVariant)
                    marginTop(MdcTheme.Spacing.sm)
                }
            }

            MdcSectionHeader("Recent Scans")
            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }
                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                    }
                    MdcRecordColumn("WiFi", { this@ScannerPage.recentWifiText }, MdcTheme.Colors.wifi, rightMargin = true)
                    MdcRecordColumn("Bluetooth", { this@ScannerPage.recentBluetoothText }, MdcTheme.Colors.bluetooth)
                }
            }

            MdcNavigationDrawerHost(
                isOpen = { this@ScannerPage.drawerOpen },
                currentPage = { "Scanner" },
                onClose = { this@ScannerPage.drawerOpen = false },
                onNavigate = { this@ScannerPage.navigateTo(it) }
            )
        }
    }

    private fun navigateTo(pageName: String) {
        drawerOpen = false
        if (pageName == "Scanner") return
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(pageName = pageName)
    }

    private fun startScanning() {
        isScanning = true
        val result = PlatformScanController.startBackgroundScanning()
        scanStatus = result.message
        if (!result.success) {
            isScanning = false
            return
        }
        refreshData()
        safeLaunch("Scanner.loop") {
            while (isScanning && isPageActive) {
                refreshData()
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    private fun stopScanning() {
        val result = PlatformScanController.stopBackgroundScanning()
        scanStatus = result.message
        isScanning = false
        refreshData()
    }

    private fun refreshData() {
        safeLaunch("Scanner.refreshData") {
            if (!isPageActive) return@safeLaunch
            runCatching {
                val db = DatabaseFactory.getDatabase()
                val wifiDao = WifiScanDao(db)
                val bluetoothDao = BluetoothScanDao(db)
                if (!isPageActive) return@safeLaunch
                wifiCount = wifiDao.getCount()
                bluetoothCount = bluetoothDao.getCount()
                recentWifiText = formatWifiRecords(wifiDao.getRecordsPaginated(limit = 10, offset = 0))
                recentBluetoothText = formatBluetoothRecords(bluetoothDao.getRecordsPaginated(limit = 10, offset = 0))
            }.onFailure { CrashLogger.log("Scanner.refreshData", it) }
        }
    }

    private fun formatWifiRecords(records: List<WifiScanRecord>): String {
        if (records.isEmpty()) return "No WiFi records"
        return records.joinToString(separator = "\n\n") {
            "${it.ssid.ifEmpty { "Unknown" }}  ${it.signalStrength} dBm\n${it.bssid}\nSeen ${it.count} times"
        }
    }

    private fun formatBluetoothRecords(records: List<BluetoothScanRecord>): String {
        if (records.isEmpty()) return "No Bluetooth records"
        return records.joinToString(separator = "\n\n") {
            "${it.name.ifEmpty { "Unknown" }}  ${it.rssi} dBm\n${it.address}\nSeen ${it.count} times"
        }
    }
}
