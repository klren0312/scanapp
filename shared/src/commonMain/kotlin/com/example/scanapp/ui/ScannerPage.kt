package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import com.example.scanapp.service.PlatformScanController
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.coroutines.delay
import com.tencent.kuikly.core.coroutines.launch
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
    private var recentWifi: List<WifiScanRecord> = emptyList()
    private var recentBluetooth: List<BluetoothScanRecord> = emptyList()

    override fun created() {
        super.created()
        refreshData()
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

            MdcTopBar("WiFi / Bluetooth Scanner") { this@ScannerPage.closePage() }

            MdcCardRow {
                MdcStatBadge("WiFi Networks", "${this@ScannerPage.wifiCount}", MdcTheme.Colors.wifi)
                MdcStatBadge("Bluetooth", "${this@ScannerPage.bluetoothCount}", MdcTheme.Colors.bluetooth)
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
                        text(
                            if (this@ScannerPage.isScanning) "Stop Scanning"
                            else "Start Scanning"
                        )
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

            MdcSectionHeader("Recent WiFi")
            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }
                this@ScannerPage.recentWifi.forEach { record ->
                    this@ScannerPage.run { root.MdcScanRecordRow(record.ssid, "${record.signalStrength} dBm") }
                }

                MdcSectionHeader("Recent Bluetooth")
                this@ScannerPage.recentBluetooth.forEach { record ->
                    this@ScannerPage.run { root.MdcScanRecordRow(record.name, "${record.rssi} dBm") }
                }
            }

            MdcDivider()

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_EVENLY)
                    padding(top = MdcTheme.Spacing.sm, bottom = MdcTheme.Spacing.sm)
                }
                this@ScannerPage.run { root.MdcNavButton("Devices", "DeviceList") }
                this@ScannerPage.run { root.MdcNavButton("Stats", "Statistics") }
                this@ScannerPage.run { root.MdcNavButton("Map", "Map") }
                this@ScannerPage.run { root.MdcNavButton("Settings", "Settings") }
            }
        }
    }

    private fun ViewContainer<*, *>.MdcScanRecordRow(name: String, detail: String) {
        MdcListItem(title = name, subtitle = "", trailing = detail)
    }

    private fun ViewContainer<*, *>.MdcNavButton(label: String, pageName: String) {
        MdcOutlinedButton(label = label) {
            this@ScannerPage.acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                .openPage(pageName = pageName)
        }
    }

    private fun closePage() {
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
    }

    private fun startScanning() {
        isScanning = true
        val result = PlatformScanController.startBackgroundScanning()
        scanStatus = result.message
        if (!result.success) {
            return
        }
        refreshData()
        lifecycleScope.launch {
            while (isScanning) {
                refreshData()
                delay(3000)
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
        lifecycleScope.launch {
            runCatching {
                val db = DatabaseFactory.getDatabase()
                val wifiDao = WifiScanDao(db)
                val bluetoothDao = BluetoothScanDao(db)
                wifiCount = wifiDao.getCount()
                bluetoothCount = bluetoothDao.getCount()
                recentWifi = wifiDao.getRecordsPaginated(limit = 10, offset = 0)
                recentBluetooth = bluetoothDao.getRecordsPaginated(limit = 10, offset = 0)
            }.onFailure { it.printStackTrace() }
        }
    }
}
