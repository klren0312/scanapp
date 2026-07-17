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
import com.example.scanapp.service.PlatformScanController
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

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
    private var mapPreviewLoading by observable(false)
    private var mapPreviewFailed by observable(false)
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
                    text("Gaode map")
                    fontSize(MdcTheme.Typography.bodySmall)
                    color(MdcTheme.Colors.onSurfaceVariant)
                    marginTop(2f)
                }
            }
            vif({ this@DeviceDetailPage.hasValidLocation() }) {
                Image {
                    attr {
                        src(this@DeviceDetailPage.buildGaodeTileUrl(this@DeviceDetailPage.currentLat, this@DeviceDetailPage.currentLon))
                        width(pagerData.pageViewWidth - 56f)
                        height(200f)
                        resizeCover()
                        borderRadius(12f)
                        overflow(true)
                        marginTop(MdcTheme.Spacing.sm)
                    }
                    event {
                        loadSuccess {
                            this@DeviceDetailPage.mapPreviewLoading = false
                            this@DeviceDetailPage.mapPreviewFailed = false
                        }
                        loadFailure {
                            this@DeviceDetailPage.mapPreviewLoading = false
                            this@DeviceDetailPage.mapPreviewFailed = true
                            CrashLogger.log(
                                "DeviceDetail.mapPreview",
                                "Gaode map tile failed to load: ${this@DeviceDetailPage.currentLat},${this@DeviceDetailPage.currentLon}"
                            )
                        }
                    }
                }
                vif({ this@DeviceDetailPage.mapPreviewLoading }) {
                    MdcCaption("Loading map preview...", MdcTheme.Colors.onSurfaceVariant)
                }
                vif({ this@DeviceDetailPage.mapPreviewFailed }) {
                    MdcBodyText("Map preview unavailable", MdcTheme.Colors.error)
                }
                MdcOutlinedButton("Open Gaode map") {
                    PlatformScanController.openDeviceMap(
                        this@DeviceDetailPage.currentLat,
                        this@DeviceDetailPage.currentLon,
                        this@DeviceDetailPage.title
                    )
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
        if (currentLat !in -90.0..90.0 || currentLon !in -180.0..180.0) return false
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

    private fun buildGaodeTileUrl(lat: Double, lon: Double): String {
        val (gaodeLat, gaodeLon) = toGaodeCoordinate(lat, lon)
        val boundedLat = gaodeLat.coerceIn(-WEB_MERCATOR_MAX_LATITUDE, WEB_MERCATOR_MAX_LATITUDE)
        val tileCount = 1 shl MAP_PREVIEW_ZOOM
        val x = floor((gaodeLon + 180.0) / 360.0 * tileCount).toInt().coerceIn(0, tileCount - 1)
        val latitudeRadians = boundedLat / 180.0 * PI
        val y = floor(
            (1.0 - ln(tan(latitudeRadians) + 1.0 / cos(latitudeRadians)) / PI) /
                2.0 * tileCount
        ).toInt().coerceIn(0, tileCount - 1)
        val host = (kotlin.math.abs(x + y) % GAODE_HOST_COUNT) + 1
        return "http://wprd0$host.is.autonavi.com/appmaptile" +
            "?x=$x&y=$y&z=$MAP_PREVIEW_ZOOM&lang=zh_cn&size=1&scl=1&style=7"
    }

    private fun toGaodeCoordinate(latitude: Double, longitude: Double): Pair<Double, Double> {
        if (longitude !in 72.004..137.8347 || latitude !in 0.8293..55.8271) {
            return latitude to longitude
        }

        var latitudeOffset = transformLatitude(longitude - 105.0, latitude - 35.0)
        var longitudeOffset = transformLongitude(longitude - 105.0, latitude - 35.0)
        val latitudeRadians = latitude / 180.0 * PI
        var magic = sin(latitudeRadians)
        magic = 1 - GCJ_EARTH_ECCENTRICITY * magic * magic
        val sqrtMagic = sqrt(magic)
        latitudeOffset = latitudeOffset * 180.0 /
            ((GCJ_EARTH_RADIUS * (1 - GCJ_EARTH_ECCENTRICITY)) / (magic * sqrtMagic) * PI)
        longitudeOffset = longitudeOffset * 180.0 /
            (GCJ_EARTH_RADIUS / sqrtMagic * cos(latitudeRadians) * PI)
        return latitude + latitudeOffset to longitude + longitudeOffset
    }

    private fun transformLatitude(x: Double, y: Double): Double {
        var result = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y +
            0.1 * x * y + 0.2 * sqrt(abs(x))
        result += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        result += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        result += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return result
    }

    private fun transformLongitude(x: Double, y: Double): Double {
        var result = 300.0 + x + 2.0 * y + 0.1 * x * x +
            0.1 * x * y + 0.1 * sqrt(abs(x))
        result += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        result += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        result += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return result
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
        currentLat = effLat
        currentLon = effLon
        updateMapPreview(effLat, effLon, record.timestamp, locations)
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
        currentLat = effLat
        currentLon = effLon
        updateMapPreview(effLat, effLon, record.timestamp, locations)
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
        currentLat = effLat
        currentLon = effLon
        updateMapPreview(effLat, effLon, record.timestamp, locations)
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
        mapPreviewLoading = false
        mapPreviewFailed = false
        if (latitude.isNaN() || latitude.isInfinite() || longitude.isNaN() || longitude.isInfinite()) {
            mapCoordinateText = "Invalid coordinates"
            mapMetaText = "Last scan timestamp: $timestamp"
            nearbyText = "No valid location data"
            return
        }
        mapPreviewLoading = true
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

    companion object {
        private const val MAP_PREVIEW_ZOOM = 15
        private const val GAODE_HOST_COUNT = 4
        private const val WEB_MERCATOR_MAX_LATITUDE = 85.05112878
        private const val GCJ_EARTH_RADIUS = 6378245.0
        private const val GCJ_EARTH_ECCENTRICITY = 0.006693421622965943
    }
}
