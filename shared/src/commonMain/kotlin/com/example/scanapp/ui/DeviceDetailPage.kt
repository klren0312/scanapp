package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.LocationDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.LocationRecord
import com.example.scanapp.models.WifiScanRecord
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.layout.FlexPositionType
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
    private var mapCoordinateText by observable("Loading coordinates...")
    private var mapMetaText by observable("Loading map data...")
    private var nearbyText by observable("Loading nearby locations...")
    private var isPageActive = true

    override fun created() {
        super.created()
        loadDevice()
    }

    override fun pageWillDestroy() {
        isPageActive = false
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

            MdcTopBar({ this@DeviceDetailPage.title }) { this@DeviceDetailPage.closePage() }

            Scroller {
                attr {
                    flex(1f)
                    marginTop(MdcTheme.Spacing.sm)
                }
                val scroller = this

                MdcSectionHeader("Device Data")
                this@DeviceDetailPage.run {
                    scroller.MdcDetailBlock({ this@DeviceDetailPage.detailText }, MdcTheme.Colors.primary)
                }

                MdcSectionHeader("Location")
                this@DeviceDetailPage.run {
                    scroller.MdcDetailBlock({ this@DeviceDetailPage.locationText }, MdcTheme.Colors.secondary)
                }

                MdcSectionHeader("Map")
                this@DeviceDetailPage.run {
                    scroller.MdcInlineMapPreview(
                        coordinate = { this@DeviceDetailPage.mapCoordinateText },
                        meta = { this@DeviceDetailPage.mapMetaText },
                        nearby = { this@DeviceDetailPage.nearbyText }
                    )
                }
                MdcOutlinedButton("Back to Devices") {
                    this@DeviceDetailPage.closePage()
                }
            }
        }
    }

    private fun ViewContainer<*, *>.MdcDetailBlock(text: () -> String, color: Color) {
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
                    this.text(text())
                    fontSize(MdcTheme.Typography.bodyMedium)
                    color(MdcTheme.Colors.onSurface)
                    lineHeight(20f)
                }
            }
        }
    }

    private fun ViewContainer<*, *>.MdcInlineMapPreview(coordinate: () -> String, meta: () -> String, nearby: () -> String) {
        MdcCard(elevation = MdcTheme.Elevation.level1) {
            Text {
                attr {
                    text(coordinate())
                    fontSize(MdcTheme.Typography.bodyLarge)
                    fontWeightSemiBold()
                    color(MdcTheme.Colors.onSurface)
                }
            }
            Text {
                attr {
                    text(meta())
                    fontSize(MdcTheme.Typography.bodySmall)
                    color(MdcTheme.Colors.onSurfaceVariant)
                    marginTop(2f)
                }
            }
            View {
                attr {
                    height(190f)
                    marginTop(MdcTheme.Spacing.sm)
                    borderRadius(12f)
                    overflow(true)
                    backgroundColor(Color(0xffE8F5E9L))
                    border(Border(1f, BorderStyle.SOLID, Color(0xffC8E6C9L)))
                    alignItems(FlexAlign.CENTER)
                    justifyContent(FlexJustifyContent.CENTER)
                }
                View {
                    attr {
                        positionType(FlexPositionType.ABSOLUTE)
                        absolutePosition(top = 52f, left = 0f, right = 0f)
                        height(16f)
                        backgroundColor(Color(0xffB0BEC5L))
                    }
                }
                View {
                    attr {
                        positionType(FlexPositionType.ABSOLUTE)
                        absolutePosition(top = 122f, left = 0f, right = 0f)
                        height(10f)
                        backgroundColor(Color(0xffCFD8DCL))
                    }
                }
                View {
                    attr {
                        positionType(FlexPositionType.ABSOLUTE)
                        absolutePosition(top = 0f, left = 88f, bottom = 0f)
                        width(12f)
                        backgroundColor(Color(0xffB0BEC5L))
                    }
                }
                View {
                    attr {
                        width(22f)
                        height(22f)
                        borderRadius(11f)
                        backgroundColor(MdcTheme.Colors.error)
                        border(Border(3f, BorderStyle.SOLID, Color.WHITE))
                        boxShadow(MdcTheme.Elevation.level2)
                    }
                }
            }
            Text {
                attr {
                    text(nearby())
                    fontSize(MdcTheme.Typography.bodySmall)
                    color(MdcTheme.Colors.onSurface)
                    lineHeight(18f)
                    marginTop(MdcTheme.Spacing.sm)
                }
            }
        }
    }

    private fun loadDevice() {
        val deviceType = pagerData.params.optString("deviceType")
        val deviceKey = pagerData.params.optString("deviceKey")
        lifecycleScope.launch {
            runCatching {
                val db = DatabaseFactory.getDatabase()
                val locations = LocationDao(db).getAllRecords()
                if (!isPageActive) return@runCatching
                if (deviceType == "wifi") {
                    val record = WifiScanDao(db).getRecordByBssid(deviceKey)
                    if (isPageActive) renderWifi(record, locations)
                } else {
                    val record = BluetoothScanDao(db).getRecordByAddress(deviceKey)
                    if (isPageActive) renderBluetooth(record, locations)
                }
            }.onFailure {
                if (!isPageActive) return@onFailure
                detailText = "Failed to load device: ${it.message}"
                locationText = "No location data"
                mapCoordinateText = "No coordinates"
                mapMetaText = "Map data unavailable"
                nearbyText = "No nearby locations"
                it.printStackTrace()
            }
        }
    }

    private fun renderWifi(record: WifiScanRecord?, locations: List<LocationRecord>) {
        if (record == null) {
            title = "WiFi Detail"
            detailText = "Device not found"
            locationText = "No location data"
            mapCoordinateText = "No coordinates"
            mapMetaText = "Device not found"
            nearbyText = "No nearby locations"
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
        updateMapPreview(record.latitude, record.longitude, record.timestamp, locations)
    }

    private fun renderBluetooth(record: BluetoothScanRecord?, locations: List<LocationRecord>) {
        if (record == null) {
            title = "Bluetooth Detail"
            detailText = "Device not found"
            locationText = "No location data"
            mapCoordinateText = "No coordinates"
            mapMetaText = "Device not found"
            nearbyText = "No nearby locations"
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
        updateMapPreview(record.latitude, record.longitude, record.timestamp, locations)
    }

    private fun buildLocationText(latitude: Double, longitude: Double, locations: List<LocationRecord>): String {
        if (latitude.isNaN() || latitude.isInfinite() || longitude.isNaN() || longitude.isInfinite()) {
            return "Invalid coordinates"
        }
        val nearby = locations.filter {
            !it.latitude.isNaN() && !it.latitude.isInfinite() &&
            !it.longitude.isNaN() && !it.longitude.isInfinite()
        }.sortedBy {
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

    private fun updateMapPreview(latitude: Double, longitude: Double, timestamp: Long, locations: List<LocationRecord>) {
        if (latitude.isNaN() || latitude.isInfinite() || longitude.isNaN() || longitude.isInfinite()) {
            mapCoordinateText = "Invalid coordinates"
            mapMetaText = "Last scan timestamp: $timestamp"
            nearbyText = "No valid location data"
            return
        }
        val nearby = locations.filter {
            !it.latitude.isNaN() && !it.latitude.isInfinite() &&
            !it.longitude.isNaN() && !it.longitude.isInfinite()
        }.sortedBy {
            abs(it.latitude - latitude) + abs(it.longitude - longitude)
        }.take(3)
        mapCoordinateText = "${formatCoordinate(latitude)}, ${formatCoordinate(longitude)}"
        mapMetaText = "Last scan timestamp: $timestamp"
        nearbyText = if (nearby.isEmpty()) {
            "No stored location samples"
        } else {
            nearby.mapIndexed { index, record ->
                "#${index + 1} ${formatCoordinate(record.latitude)}, ${formatCoordinate(record.longitude)} " +
                    "accuracy ${formatOneDecimal(record.accuracy.toDouble())} m"
            }.joinToString("\n")
        }
    }

    private fun closePage() {
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
    }

    private fun formatCoordinate(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "N/A"
        val millionths = kotlin.math.round(value * 1_000_000.0).toLong()
        val sign = if (millionths < 0) "-" else ""
        val absolute = kotlin.math.abs(millionths)
        val whole = absolute / 1_000_000
        val fraction = (absolute % 1_000_000).toString().padStart(6, '0')
        return "$sign$whole.$fraction"
    }

    private fun formatOneDecimal(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "N/A"
        val tenths = kotlin.math.round(value * 10.0).toLong()
        val sign = if (tenths < 0) "-" else ""
        val absolute = kotlin.math.abs(tenths)
        return "$sign${absolute / 10}.${absolute % 10}"
    }
}
