package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.CellScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.logging.CrashLogger
import com.example.scanapp.service.PlatformScanController
import com.example.scanapp.service.cellReadinessHint
import com.example.scanapp.service.requestCellScanPermission
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("Scanner")
class ScannerPage : Pager() {

    private var isScanning by observable(false)
    private var scanStatus by observable("")
    private var wifiCount by observable(0L)
    private var bluetoothCount by observable(0L)
    private var cellCount by observable(0L)
    private var cellHint by observable("")
    private var drawerOpen by observable(false)
    private var isPageActive = true
    private var hasAppeared = false

    override fun created() {
        super.created()
        refreshCounts()
    }

    override fun pageWillDestroy() {
        isPageActive = false
        isScanning = false
        super.pageWillDestroy()
    }

    override fun pageDidAppear() {
        super.pageDidAppear()
        if (hasAppeared) {
            refreshCounts()
        } else {
            hasAppeared = true
        }
    }

    override fun body(): ViewContainer<*, *>.() -> Unit = {
        View {
            attr {
                size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                backgroundColor(MdcTheme.Colors.background)
                flexDirection(FlexDirection.COLUMN)
                padding(MdcTheme.Spacing.md)
            }

            MdcMenuTopBar("Scanner") { this@ScannerPage.drawerOpen = true }

            MdcCardRow {
                MdcStatBadge("WiFi", { "${this@ScannerPage.wifiCount}" }, MdcTheme.Colors.wifi)
                MdcStatBadge("Bluetooth", { "${this@ScannerPage.bluetoothCount}" }, MdcTheme.Colors.bluetooth)
                MdcStatBadge("Cell", { "${this@ScannerPage.cellCount}" }, MdcTheme.Colors.cell)
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
                        this@ScannerPage.isScanning && this@ScannerPage.cellHint.isNotEmpty() -> this@ScannerPage.cellHint
                        this@ScannerPage.isScanning -> "Scanning... counts refresh every few seconds"
                        else -> "Device details are available in the Device List page"
                    }
                    text(message)
                    fontSize(MdcTheme.Typography.bodyLarge)
                    color(if (this@ScannerPage.isScanning) MdcTheme.Colors.warning else MdcTheme.Colors.onSurfaceVariant)
                    marginTop(MdcTheme.Spacing.sm)
                }
            }

            vif({ this@ScannerPage.isScanning && this@ScannerPage.cellHint.contains("location permission") }) {
                View {
                    attr {
                        marginTop(MdcTheme.Spacing.sm)
                        padding(top = 10f, bottom = 10f, left = 14f, right = 14f)
                        backgroundColor(MdcTheme.Colors.primary)
                        borderRadius(16f)
                        alignItems(FlexAlign.CENTER)
                        justifyContent(FlexJustifyContent.CENTER)
                    }
                    event {
                        click { this@ScannerPage.requestCellPermission() }
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

            View {
                attr {
                    marginTop(MdcTheme.Spacing.md)
                    padding(MdcTheme.Spacing.md)
                    backgroundColor(MdcTheme.Colors.surface)
                    borderRadius(16f)
                    boxShadow(MdcTheme.Elevation.level1)
                }
                Text {
                    attr {
                        text("Live counts only")
                        fontSize(MdcTheme.Typography.titleMedium)
                        fontWeightSemiBold()
                        color(MdcTheme.Colors.onSurface)
                    }
                }
                Text {
                    attr {
                        text("To reduce page jank, this screen no longer renders scan result cards. Open Device List to browse WiFi, Bluetooth, and Cell records.")
                        fontSize(MdcTheme.Typography.bodyMedium)
                        color(MdcTheme.Colors.onSurfaceVariant)
                        marginTop(MdcTheme.Spacing.sm)
                    }
                }
                View {
                    attr {
                        marginTop(MdcTheme.Spacing.md)
                        padding(top = 10f, bottom = 10f, left = 14f, right = 14f)
                        backgroundColor(MdcTheme.Colors.primary)
                        borderRadius(16f)
                        alignItems(FlexAlign.CENTER)
                        justifyContent(FlexJustifyContent.CENTER)
                    }
                    event {
                        click {
                            this@ScannerPage.navigateTo("DeviceList")
                        }
                    }
                    Text {
                        attr {
                            text("Open Device List")
                            fontSize(MdcTheme.Typography.labelLarge)
                            fontWeightSemiBold()
                            color(MdcTheme.Colors.onPrimary)
                        }
                    }
                }
            }

            MdcNavigationDrawerHost(
                isOpen = { this@ScannerPage.drawerOpen },
                currentPage = { "Scanner" },
                pageHeight = this@ScannerPage.pagerData.pageViewHeight,
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
        if (!PlatformScanController.isBluetoothEnabled()) {
            scanStatus = "Requesting Bluetooth..."
            PlatformScanController.requestEnableBluetooth(onEnabled = {
                if (isPageActive) startScanningInternal()
            })
            return
        }
        startScanningInternal()
    }

    private fun startScanningInternal() {
        isScanning = true
        val result = PlatformScanController.startBackgroundScanning()
        scanStatus = result.message
        if (!result.success) {
            isScanning = false
            return
        }
        refreshCounts()
        safeLaunch("Scanner.loop") {
            while (isScanning && isPageActive) {
                refreshCounts()
                // Count-only refresh is cheaper than rebuilding a device list every second.
                kotlinx.coroutines.delay(3000L)
            }
        }
    }

    private fun stopScanning() {
        val result = PlatformScanController.stopBackgroundScanning()
        scanStatus = result.message
        isScanning = false
        refreshCounts()
    }

    private fun refreshCounts() {
        safeLaunch("Scanner.refreshCounts") {
            if (!isPageActive) return@safeLaunch
            runCatching {
                val db = DatabaseFactory.getDatabase()
                val wifiDao = WifiScanDao(db)
                val bluetoothDao = BluetoothScanDao(db)
                val cellDao = CellScanDao(db)
                if (!isPageActive) return@safeLaunch
                wifiCount = wifiDao.getCount()
                bluetoothCount = bluetoothDao.getCount()
                cellCount = cellDao.getCount()
                cellHint = computeCellHint(cellCount)
            }.onFailure { CrashLogger.log("Scanner.refreshCounts", it) }
        }
    }


    private fun requestCellPermission() {
        requestCellScanPermission()
    }
    private fun computeCellHint(count: Long): String = cellReadinessHint(count)
}

