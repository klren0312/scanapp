package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.CellScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.logging.CrashLogger
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.CellScanRecord
import com.example.scanapp.models.WifiScanRecord
import com.example.scanapp.service.PlatformScanController
import com.example.scanapp.util.diffUpdate
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("Scanner")
class ScannerPage : Pager() {

    private data class MergedDevice(
        val type: String,
        val title: String,
        val identity: String,
        val primaryMetric: String,
        val secondaryMetric: String,
        val count: Int,
        val timestamp: Long,
        val tag: String,
        val tagColor: Color
    )

    private var isScanning by observable(false)
    private var scanStatus by observable("")
    private var wifiCount by observable(0L)
    private var bluetoothCount by observable(0L)
    private var cellCount by observable(0L)
    private var mergedDevices by observableList<MergedDevice>()
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

    override fun pageDidAppear() {
        super.pageDidAppear()
        refreshData()
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
            List {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }
                vforLazy({ this@ScannerPage.mergedDevices }) { item, _, _ ->
                    safe("Scanner.vforLazy") {
                        MdcDeviceCard(
                            title = item.title,
                            identity = item.identity,
                            primaryMetric = item.primaryMetric,
                            secondaryMetric = item.secondaryMetric,
                            count = item.count,
                            color = item.tagColor,
                            tag = item.tag,
                            tagColor = item.tagColor,
                            onClick = {}
                        )
                    }
                }
                vif({ this@ScannerPage.mergedDevices.isEmpty() }) {
                    MdcBodyText("No devices scanned yet", MdcTheme.Colors.onSurfaceVariant)
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
        if (!PlatformScanController.isBluetoothEnabled()) {
            scanStatus = "正在请求开启蓝牙..."
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
                val cellDao = CellScanDao(db)
                if (!isPageActive) return@safeLaunch
                val wifiRecords = wifiDao.getRecordsPaginated(limit = 20, offset = 0)
                val bluetoothRecords = bluetoothDao.getRecordsPaginated(limit = 20, offset = 0)
                val cellRecords = cellDao.getRecordsPaginatedOrderedBySignal(limit = 20, offset = 0)
                if (!isPageActive) return@safeLaunch
                wifiCount = wifiDao.getCount()
                bluetoothCount = bluetoothDao.getCount()
                cellCount = cellDao.getCount()
                this@ScannerPage.mergedDevices.diffUpdate(buildMergedDevices(wifiRecords, bluetoothRecords, cellRecords))
            }.onFailure { CrashLogger.log("Scanner.refreshData", it) }
        }
    }

    private fun buildMergedDevices(
        wifi: List<WifiScanRecord>,
        bluetooth: List<BluetoothScanRecord>,
        cell: List<CellScanRecord>
    ): List<MergedDevice> {
        val list = mutableListOf<MergedDevice>()
        wifi.forEach {
            list.add(
                MergedDevice(
                    type = "wifi",
                    title = it.ssid.ifEmpty { "Unknown" },
                    identity = it.bssid,
                    primaryMetric = "${it.signalStrength} dBm",
                    secondaryMetric = "${it.frequency} MHz",
                    count = it.count,
                    timestamp = it.timestamp,
                    tag = "WiFi",
                    tagColor = MdcTheme.Colors.wifi
                )
            )
        }
        bluetooth.forEach {
            list.add(
                MergedDevice(
                    type = "bluetooth",
                    title = it.name.ifEmpty { "Unknown" },
                    identity = it.address,
                    primaryMetric = "${it.rssi} dBm",
                    secondaryMetric = it.deviceType,
                    count = it.count,
                    timestamp = it.timestamp,
                    tag = "Bluetooth",
                    tagColor = MdcTheme.Colors.bluetooth
                )
            )
        }
        cell.forEach {
            list.add(
                MergedDevice(
                    type = "cell",
                    title = if (it.operator.isNotEmpty() && it.operator != "Unknown") it.operator else "${it.networkType} Cell",
                    identity = it.cellKey,
                    primaryMetric = "${it.signalStrength} dBm",
                    secondaryMetric = it.networkType,
                    count = it.count,
                    timestamp = it.timestamp,
                    tag = "Cell",
                    tagColor = MdcTheme.Colors.cell
                )
            )
        }
        list.sortByDescending { it.timestamp }
        return list.take(40)
    }
}
