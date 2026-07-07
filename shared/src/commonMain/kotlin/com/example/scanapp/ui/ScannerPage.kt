package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.coroutines.delay
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.View

@Page("Scanner")
class ScannerPage : Pager() {

    private var isScanning = false
    private var wifiCount = 0L
    private var bluetoothCount = 0L
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

            MdcTitle("WiFi / Bluetooth Scanner")

            MdcCardRow {
                MdcStatBadge("WiFi Networks", "${this@ScannerPage.wifiCount}", MdcTheme.Colors.wifi)
                MdcStatBadge("Bluetooth", "${this@ScannerPage.bluetoothCount}", MdcTheme.Colors.bluetooth)
            }

            MdcFilledButton(
                label = if (this@ScannerPage.isScanning) "Stop Scanning" else "Start Scanning"
            ) {
                if (this@ScannerPage.isScanning) this@ScannerPage.stopScanning() else this@ScannerPage.startScanning()
            }

            if (this@ScannerPage.isScanning) {
                MdcBodyText("Scanning...", MdcTheme.Colors.warning)
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

    private fun startScanning() {
        isScanning = true
        lifecycleScope.launch {
            while (isScanning) {
                refreshData()
                delay(3000)
            }
        }
    }

    private fun stopScanning() {
        isScanning = false
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
