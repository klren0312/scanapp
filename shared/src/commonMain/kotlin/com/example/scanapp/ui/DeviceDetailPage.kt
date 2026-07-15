package com.example.scanapp.ui

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.CellScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.LocationDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.logging.CrashLogger
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.CellScanRecord
import com.example.scanapp.models.LocationRecord
import com.example.scanapp.models.WifiScanRecord
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
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
    private var currentLat by observable(0.0)
    private var currentLon by observable(0.0)
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
                    scroller.MdcRealMapCard(
                        coordinate = { this@DeviceDetailPage.mapCoordinateText },
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

    private fun ViewContainer<*, *>.MdcRealMapCard(coordinate: () -> String, nearby: () -> String) {
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
                    text("真实地图（OpenStreetMap）")
                    fontSize(MdcTheme.Typography.bodySmall)
                    color(MdcTheme.Colors.onSurfaceVariant)
                    marginTop(2f)
                }
            }
            vif({ this@DeviceDetailPage.hasValidLocation() }) {
                Image {
                    attr {
                        src(this@DeviceDetailPage.buildStaticMapUrl(this@DeviceDetailPage.currentLat, this@DeviceDetailPage.currentLon))
                        width(pagerData.pageViewWidth - 56f)
                        height(200f)
                        borderRadius(12f)
                        overflow(true)
                        marginTop(MdcTheme.Spacing.sm)
                    }
                }
            }
            vif({ !this@DeviceDetailPage.hasValidLocation() }) {
                Text {
                    attr {
                        text("无有效坐标，无法显示地图")
                        fontSize(MdcTheme.Typography.bodySmall)
                        color(MdcTheme.Colors.onSurfaceVariant)
                        marginTop(MdcTheme.Spacing.sm)
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

    private fun hasValidLocation(): Boolean {
        if (currentLat.isNaN() || currentLat.isInfinite()) return false
        if (currentLon.isNaN() || currentLon.isInfinite()) return false
        return !(currentLat == 0.0 && currentLon == 0.0)
    }

    // Prefer the device record's own coordinates; if they are missing/invalid, fall back
    // to the nearest stored location sample so the map can still be shown.
    private fun resolveLocation(
        recordLat: Double,
        recordLon: Double,
        locations: List<LocationRecord>
    ): Pair<Double, Double> {
        val recordValid = !(recordLat.isNaN() || recordLat.isInfinite() ||
            recordLon.isNaN() || recordLon.isInfinite() ||
            (recordLat == 0.0 && recordLon == 0.0))
        if (recordValid) return recordLat to recordLon

        val nearest = locations
            .filter {
                !it.latitude.isNaN() && !it.latitude.isInfinite() &&
                    !it.longitude.isNaN() && !it.longitude.isInfinite() &&
                    !(it.latitude == 0.0 && it.longitude == 0.0)
            }
            .minByOrNull { abs(it.latitude - recordLat) + abs(it.longitude - recordLon) }
        return if (nearest != null) nearest.latitude to nearest.longitude else 0.0 to 0.0
    }

    private fun buildStaticMapUrl(lat: Double, lon: Double): String {
        val center = "$lat,$lon"
        return "https://staticmap.openstreetmap.de/staticmap.php" +
            "?center=$center&zoom=15&size=600x300&maptype=mapnik&markers=$center,red"
    }

    private fun loadDevice() {
        val deviceType = pagerData.params.optString("deviceType")
        val deviceKey = pagerData.params.optString("deviceKey")
        safeLaunch("DeviceDetail.loadDevice") {
            runCatching {
                val db = DatabaseFactory.getDatabase()
                val locations = LocationDao(db).getAllRecords()
                if (!isPageActive) return@runCatching
                if (deviceType == "wifi") {
                    val record = WifiScanDao(db).getRecordByBssid(deviceKey)
                    if (isPageActive) renderWifi(record, locations)
                } else if (deviceType == "cell") {
                    val record = CellScanDao(db).getRecordByCellKey(deviceKey)
                    if (isPageActive) renderCell(record, locations)
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
                CrashLogger.log("DeviceDetail.loadDevice", it)
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
        val (effLat, effLon) = resolveLocation(record.latitude, record.longitude, locations)
        updateMapPreview(effLat, effLon, record.timestamp, locations)
        currentLat = effLat
        currentLon = effLon
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
        val (effLat, effLon) = resolveLocation(record.latitude, record.longitude, locations)
        updateMapPreview(effLat, effLon, record.timestamp, locations)
        currentLat = effLat
        currentLon = effLon
    }

    private fun renderCell(record: CellScanRecord?, locations: List<LocationRecord>) {
        if (record == null) {
            title = "Cell Detail"
            detailText = "Device not found"
            locationText = "No location data"
            mapCoordinateText = "No coordinates"
            mapMetaText = "Device not found"
            nearbyText = "No nearby locations"
            return
        }
        title = if (record.operator.isNotEmpty() && record.operator != "Unknown") record.operator else "Cell Detail"
        detailText = listOf(
            "Type: Cell",
            "Network: ${record.networkType}",
            "Operator: ${record.operator.ifEmpty { "Unknown" }}",
            "MCC: ${record.mcc}",
            "MNC: ${record.mnc}",
            "LAC/TAC: ${record.lac}",
            "CID/CI: ${record.cid}",
            "Signal: ${record.signalStrength} dBm",
            "Seen: ${record.count} times",
            "Last timestamp: ${record.timestamp}"
        ).joinToString("\n")
        locationText = buildLocationText(record.latitude, record.longitude, locations)
        val (effLat, effLon) = resolveLocation(record.latitude, record.longitude, locations)
        updateMapPreview(effLat, effLon, record.timestamp, locations)
        currentLat = effLat
        currentLon = effLon
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
