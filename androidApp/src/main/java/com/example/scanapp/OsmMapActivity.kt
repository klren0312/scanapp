package com.example.scanapp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class OsmMapActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var mapView: MapView
    private lateinit var statusView: TextView
    private var loadJob: Job? = null
    private var showingSinglePoint = false

    private val backgroundColor = 0xFF0D0F10.toInt()
    private val surfaceColor = 0xFF171A1C.toInt()
    private val onSurfaceColor = 0xFFE7ECEE.toInt()
    private val wifiColor = 0xFF62B5FF.toInt()
    private val bluetoothColor = 0xFF64D8CB.toInt()
    private val gaodeTileSource = object : OnlineTileSourceBase(
        "Gaode",
        1,
        20,
        256,
        ".png",
        arrayOf(
            "http://wprd01.is.autonavi.com/appmaptile?",
            "http://wprd02.is.autonavi.com/appmaptile?",
            "http://wprd03.is.autonavi.com/appmaptile?",
            "http://wprd04.is.autonavi.com/appmaptile?"
        )
    ) {
        override fun getTileURLString(mapTileIndex: Long): String {
            val x = MapTileIndex.getX(mapTileIndex)
            val y = MapTileIndex.getY(mapTileIndex)
            val z = MapTileIndex.getZoom(mapTileIndex)
            return "${baseUrl}x=$x&y=$y&z=$z&lang=zh_cn&size=1&scl=1&style=7"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Map"

        val targetLat = intent.getDoubleExtra(EXTRA_LAT, Double.NaN)
        val targetLon = intent.getDoubleExtra(EXTRA_LON, Double.NaN)
        val targetTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }

        mapView = MapView(this).apply {
            setTileSource(gaodeTileSource)
            setMultiTouchControls(true)
            setBackgroundColor(backgroundColor)
            controller.setZoom(3.0)
            controller.setCenter(GeoPoint(0.0, 0.0))
        }

        statusView = TextView(this).apply {
            setTextColor(onSurfaceColor)
            setBackgroundColor(0xE6171A1C.toInt())
            textSize = 14f
            setPadding(24, 12, 24, 12)
            text = "Loading devices..."
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(backgroundColor)
            addView(
                mapView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            addView(
                statusView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.START
                ).apply {
                    setMargins(24, dp(84), 24, 24)
                }
            )
            addView(
                createBackButton(),
                FrameLayout.LayoutParams(
                    dp(48),
                    dp(48),
                    Gravity.TOP or Gravity.START
                ).apply {
                    setMargins(24, 24, 24, 24)
                }
            )
        }
        setContentView(root)

        if (!targetLat.isNaN() && !targetLon.isNaN()) {
            showSinglePoint(targetLat, targetLon, targetTitle)
        } else {
            startPolling()
        }
    }

    private fun showSinglePoint(latitude: Double, longitude: Double, title: String) {
        showingSinglePoint = true
        mapView.overlays.clear()
        mapView.overlays.add(
            makeMarker(
                latitude,
                longitude,
                title.ifEmpty { "Device" },
                "$latitude, $longitude",
                bluetoothColor
            )
        )
        mapView.controller.setZoom(16.0)
        mapView.controller.setCenter(toGaodePoint(latitude, longitude))
        mapView.invalidate()
        statusView.text = if (title.isNotEmpty()) "查看设备: $title" else "Device location"
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (!showingSinglePoint) startPolling()
    }

    override fun onPause() {
        mapView.onPause()
        stopPolling()
        super.onPause()
    }

    override fun onDestroy() {
        stopPolling()
        scope.cancel()
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun startPolling() {
        if (loadJob?.isActive == true) return
        loadJob = scope.launch {
            loadDevices()
        }
    }

    private fun stopPolling() {
        loadJob?.cancel()
        loadJob = null
    }

    private suspend fun loadDevices() {
        try {
            val db = DatabaseFactory.getDatabase()
            val wifi = WifiScanDao(db).getAllRecords()
            val bluetooth = BluetoothScanDao(db).getAllRecords()
            renderDevices(wifi, bluetooth)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            statusView.text = "Failed to load devices: ${error.message ?: "unknown error"}"
        }
    }

    private fun renderDevices(wifi: List<WifiScanRecord>, bluetooth: List<BluetoothScanRecord>) {
        val validWifi = wifi.filter { hasValidLocation(it.latitude, it.longitude) }
        val validBluetooth = bluetooth.filter { hasValidLocation(it.latitude, it.longitude) }

        mapView.overlays.clear()

        validWifi.forEach { record ->
            mapView.overlays.add(
                makeMarker(
                    record.latitude,
                    record.longitude,
                    record.ssid.ifEmpty { "Unknown" },
                    "${record.signalStrength} dBm\n${record.bssid}",
                    wifiColor
                )
            )
        }

        validBluetooth.forEach { record ->
            mapView.overlays.add(
                makeMarker(
                    record.latitude,
                    record.longitude,
                    record.name.ifEmpty { "Unknown" },
                    "${record.rssi} dBm\n${record.address}",
                    bluetoothColor
                )
            )
        }

        val points = validWifi.map { toGaodePoint(it.latitude, it.longitude) } +
            validBluetooth.map { toGaodePoint(it.latitude, it.longitude) }

        if (points.isNotEmpty()) {
            if (points.size == 1) {
                mapView.controller.setZoom(16.0)
                mapView.controller.setCenter(points.first())
            } else {
                mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true)
            }
            statusView.text = "WiFi: ${validWifi.size}  Bluetooth: ${validBluetooth.size}"
        } else {
            mapView.controller.setZoom(3.0)
            mapView.controller.setCenter(GeoPoint(0.0, 0.0))
            statusView.text = "No device records with location"
        }

        mapView.invalidate()
    }

    private fun makeMarker(
        latitude: Double,
        longitude: Double,
        title: String,
        snippet: String,
        color: Int
    ): Marker {
        return Marker(mapView).apply {
            position = toGaodePoint(latitude, longitude)
            this.title = title
            this.snippet = snippet
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = makeMarkerIcon(color)
        }
    }

    private fun makeMarkerIcon(color: Int): Drawable {
        val size = dp(24)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = (size / 2f) - dp(2)
        val fill = Paint().apply {
            isAntiAlias = true
            this.color = color
        }
        val border = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
            this.color = onSurfaceColor
        }
        canvas.drawCircle(size / 2f, size / 2f, radius, fill)
        canvas.drawCircle(size / 2f, size / 2f, radius, border)
        return BitmapDrawable(resources, bitmap)
    }

    private fun hasValidLocation(latitude: Double, longitude: Double): Boolean {
        if (latitude == 0.0 && longitude == 0.0) return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun toGaodePoint(latitude: Double, longitude: Double): GeoPoint {
        if (longitude !in 72.004..137.8347 || latitude !in 0.8293..55.8271) {
            return GeoPoint(latitude, longitude)
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
            (GCJ_EARTH_RADIUS / sqrtMagic * kotlin.math.cos(latitudeRadians) * PI)
        return GeoPoint(latitude + latitudeOffset, longitude + longitudeOffset)
    }

    private fun transformLatitude(x: Double, y: Double): Double {
        var result = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y +
            0.1 * x * y + 0.2 * sqrt(kotlin.math.abs(x))
        result += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        result += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        result += (160.0 * sin(y / 12.0 * PI) + 320 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return result
    }

    private fun transformLongitude(x: Double, y: Double): Double {
        var result = 300.0 + x + 2.0 * y + 0.1 * x * x +
            0.1 * x * y + 0.1 * sqrt(kotlin.math.abs(x))
        result += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        result += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        result += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return result
    }

    private fun createBackButton(): TextView {
        return TextView(this).apply {
            text = "\u2190"
            textSize = 28f
            contentDescription = "Back"
            includeFontPadding = false
            setTextColor(onSurfaceColor)
            setBackgroundColor(surfaceColor)
            gravity = Gravity.CENTER
            setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val GCJ_EARTH_RADIUS = 6378245.0
        private const val GCJ_EARTH_ECCENTRICITY = 0.006693421622965943
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
        const val EXTRA_TITLE = "extra_title"
    }
}
