package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.LocationDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.LocationRecord
import com.example.scanapp.models.WifiScanRecord
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import kotlin.math.abs

@Page("DeviceDetail")
class DeviceDetailPage : Pager() {

    private var title by observable("Device Detail")
    private var detailText by observable("Loading device...")
    private var locationText by observable("Loading location...")
    private var drawerOpen by observable(false)
    private var targetPage by observable("DeviceList")

    override fun created() {
        super.created()
        loadDevice()
    }

    override fun body(): ViewContainer<*, *>.() -> Unit = {
        View {
            attr {
                size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                backgroundColor(MdcTheme.Colors.background)
                flexDirection(FlexDirection.COLUMN)
                padding(MdcTheme.Spacing.md)
            }

            MdcMenuTopBar(this@DeviceDetailPage.title) { this@DeviceDetailPage.drawerOpen = true }

            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }
                val scroller = this

                MdcSectionHeader("Device Data")
                this@DeviceDetailPage.run {
                    scroller.MdcDetailBlock(this@DeviceDetailPage.detailText, MdcTheme.Colors.primary)
                }

                MdcSectionHeader("Location")
                this@DeviceDetailPage.run {
                    scroller.MdcDetailBlock(this@DeviceDetailPage.locationText, MdcTheme.Colors.secondary)
                }

                MdcFilledButton("Open Map") {
                    this@DeviceDetailPage.acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                        .openPage(pageName = "Map")
                }
                MdcOutlinedButton("Back to Devices") {
                    this@DeviceDetailPage.closePage()
                }
            }

            MdcNavigationDrawerHost(
                isOpen = { this@DeviceDetailPage.drawerOpen },
                currentPage = { this@DeviceDetailPage.targetPage },
                onClose = { this@DeviceDetailPage.drawerOpen = false },
                onNavigate = { this@DeviceDetailPage.navigateTo(it) }
            )
        }
    }

    private fun ViewContainer<*, *>.MdcDetailBlock(text: String, color: Color) {
        MdcCard(elevation = MdcTheme.Elevation.level1) {
            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.SPACE_BETWEEN)
                    marginBottom(MdcTheme.Spacing.sm)
                }
                Text {
                    attr {
                        this.text("Summary")
                        fontSize(MdcTheme.Typography.titleSmall)
                        fontWeightSemiBold()
                        this.color(color)
                    }
                }
            }
            Text {
                attr {
                    this.text(text)
                    fontSize(MdcTheme.Typography.bodyMedium)
                    color(MdcTheme.Colors.onSurface)
                    lineHeight(20f)
                }
            }
        }
    }

    private fun loadDevice() {
        val deviceType = pagerData.params.optString("deviceType")
        val deviceKey = pagerData.params.optString("deviceKey")
        targetPage = "DeviceList"
        lifecycleScope.launch {
            runCatching {
                val db = DatabaseFactory.getDatabase()
                val locations = LocationDao(db).getAllRecords()
                if (deviceType == "wifi") {
                    val record = WifiScanDao(db).getRecordByBssid(deviceKey)
                    renderWifi(record, locations)
                } else {
                    val record = BluetoothScanDao(db).getRecordByAddress(deviceKey)
                    renderBluetooth(record, locations)
                }
            }.onFailure {
                detailText = "Failed to load device: ${it.message}"
                locationText = "No location data"
                it.printStackTrace()
            }
        }
    }

    private fun renderWifi(record: WifiScanRecord?, locations: List<LocationRecord>) {
        if (record == null) {
            title = "WiFi Detail"
            detailText = "Device not found"
            locationText = "No location data"
            return
        }
        title = record.ssid.ifEmpty { "WiFi Detail" }
        detailText = listOf(
            "Type: WiFi",
            "SSID: ${record.ssid.ifEmpty { "Unknown" }}",
            "BSSID: ${record.bssid}",
            "Signal: ${record.signalStrength} dBm",
            "Frequency: ${record.frequency} MHz",
            "Seen: ${record.count} times",
            "Last timestamp: ${record.timestamp}"
        ).joinToString("\n")
        locationText = buildLocationText(record.latitude, record.longitude, locations)
    }

    private fun renderBluetooth(record: BluetoothScanRecord?, locations: List<LocationRecord>) {
        if (record == null) {
            title = "Bluetooth Detail"
            detailText = "Device not found"
            locationText = "No location data"
            return
        }
        title = record.name.ifEmpty { "Bluetooth Detail" }
        detailText = listOf(
            "Type: Bluetooth",
            "Name: ${record.name.ifEmpty { "Unknown" }}",
            "Address: ${record.address}",
            "RSSI: ${record.rssi} dBm",
            "Device type: ${record.deviceType}",
            "Seen: ${record.count} times",
            "Last timestamp: ${record.timestamp}"
        ).joinToString("\n")
        locationText = buildLocationText(record.latitude, record.longitude, locations)
    }

    private fun buildLocationText(latitude: Double, longitude: Double, locations: List<LocationRecord>): String {
        val nearby = locations.sortedBy {
            abs(it.latitude - latitude) + abs(it.longitude - longitude)
        }.take(3)
        val base = mutableListOf(
            "Last scan latitude: ${formatCoordinate(latitude)}",
            "Last scan longitude: ${formatCoordinate(longitude)}"
        )
        if (nearby.isEmpty()) {
            base.add("No stored location samples")
            return base.joinToString("\n")
        }
        base.add("")
        base.add("Nearest stored locations:")
        nearby.forEachIndexed { index, record ->
            base.add(
                "#${index + 1}: ${formatCoordinate(record.latitude)}, ${formatCoordinate(record.longitude)} " +
                    "accuracy ${formatOneDecimal(record.accuracy.toDouble())} m, timestamp ${record.timestamp}"
            )
        }
        return base.joinToString("\n")
    }

    private fun navigateTo(pageName: String) {
        drawerOpen = false
        if (pageName == "DeviceList") {
            closePage()
            return
        }
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(pageName = pageName)
        closePage()
    }

    private fun closePage() {
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
    }

    private fun formatCoordinate(value: Double): String {
        val millionths = kotlin.math.round(value * 1_000_000.0).toLong()
        val sign = if (millionths < 0) "-" else ""
        val absolute = kotlin.math.abs(millionths)
        val whole = absolute / 1_000_000
        val fraction = (absolute % 1_000_000).toString().padStart(6, '0')
        return "$sign$whole.$fraction"
    }

    private fun formatOneDecimal(value: Double): String {
        val tenths = kotlin.math.round(value * 10.0).toLong()
        val sign = if (tenths < 0) "-" else ""
        val absolute = kotlin.math.abs(tenths)
        return "$sign${absolute / 10}.${absolute % 10}"
    }
}
