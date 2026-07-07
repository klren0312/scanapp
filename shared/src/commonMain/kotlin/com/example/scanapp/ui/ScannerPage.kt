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
import com.tencent.kuikly.core.views.Text
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
                backgroundColor(Color("#F5F5F5"))
                flexDirection(FlexDirection.COLUMN)
                padding(16f)
            }

            TitleText("WiFi / Bluetooth Scanner")

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_AROUND)
                    marginTop(20f)
                    padding(12f)
                    backgroundColor(Color.WHITE)
                    borderRadius(8f)
                }
                InfoText("WiFi: ${this@ScannerPage.wifiCount}", Color("#2196F3"))
                InfoText("Bluetooth: ${this@ScannerPage.bluetoothCount}", Color("#4CAF50"))
            }

            ActionButton(
                label = if (this@ScannerPage.isScanning) "Stop Scanning" else "Start Scanning",
                background = if (this@ScannerPage.isScanning) Color("#F44336") else Color("#2196F3")
            ) {
                if (this@ScannerPage.isScanning) this@ScannerPage.stopScanning() else this@ScannerPage.startScanning()
            }

            if (this@ScannerPage.isScanning) {
                InfoText("Scanning...", Color("#FF9800"))
            }

            InfoText("Recent WiFi", Color("#333333"))
            Scroller {
                attr {
                    flex(1f)
                    marginTop(8f)
                }
                this@ScannerPage.recentWifi.forEach { record ->
                    this@ScannerPage.run { root.ScanRecordRow(record.ssid, "${record.signalStrength} dBm") }
                }

                InfoText("Recent Bluetooth", Color("#333333"))
                this@ScannerPage.recentBluetooth.forEach { record ->
                    this@ScannerPage.run { root.ScanRecordRow(record.name, "${record.rssi} dBm") }
                }
            }

            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_AROUND)
                    marginTop(8f)
                }
                this@ScannerPage.run { root.NavButton("Devices", "DeviceList") }
                this@ScannerPage.run { root.NavButton("Stats", "Statistics") }
                this@ScannerPage.run { root.NavButton("Map", "Map") }
                this@ScannerPage.run { root.NavButton("Settings", "Settings") }
            }
        }
    }

    private fun ViewContainer<*, *>.ScanRecordRow(name: String, detail: String) {
        View {
            attr {
                flexDirection(FlexDirection.ROW)
                justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                padding(8f)
                marginTop(4f)
                backgroundColor(Color.WHITE)
                borderRadius(6f)
            }
            Text {
                attr {
                    text(name.ifEmpty { "Unknown" })
                    fontSize(14f)
                    color(Color("#333333"))
                }
            }
            Text {
                attr {
                    text(detail)
                    fontSize(13f)
                    color(Color("#999999"))
                }
            }
        }
    }

    private fun ViewContainer<*, *>.NavButton(label: String, pageName: String) {
        ActionButton(label = label, background = Color("#E3F2FD"), textColor = Color("#2196F3")) {
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
