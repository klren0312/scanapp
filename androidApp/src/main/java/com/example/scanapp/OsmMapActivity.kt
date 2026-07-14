package com.example.scanapp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class OsmMapActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var mapView: MapView
    private lateinit var statusView: TextView
    private lateinit var drawerOverlay: FrameLayout
    private var pollingJob: Job? = null
    private var showingSinglePoint = false

    private val wifiColor = 0xFF1565C0.toInt()
    private val bluetoothColor = 0xFF6A1B9A.toInt()

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
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(3.0)
            controller.setCenter(GeoPoint(0.0, 0.0))
        }

        statusView = TextView(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(0x99000000.toInt())
            textSize = 14f
            setPadding(24, 12, 24, 12)
            text = "Loading devices..."
        }

        val root = FrameLayout(this).apply {
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
                    setMargins(24, 24, 24, 24)
                }
            )
            addView(
                createMenuButton(),
                FrameLayout.LayoutParams(
                    dp(72),
                    dp(40),
                    Gravity.TOP or Gravity.END
                ).apply {
                    setMargins(24, 24, 24, 24)
                }
            )
            drawerOverlay = createDrawerOverlay()
            addView(
                drawerOverlay,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
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
        mapView.controller.setCenter(GeoPoint(latitude, longitude))
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
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            loadDevices()
            while (true) {
                delay(3000)
                loadDevices()
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun loadDevices() {
        scope.launch {
            runCatching {
                val db = withContext(Dispatchers.IO) { DatabaseFactory.getDatabase() }
                val wifi = withContext(Dispatchers.IO) { WifiScanDao(db).getAllRecords() }
                val bluetooth = withContext(Dispatchers.IO) { BluetoothScanDao(db).getAllRecords() }
                wifi to bluetooth
            }.onSuccess { (wifi, bluetooth) ->
                renderDevices(wifi, bluetooth)
            }.onFailure { error ->
                statusView.text = "Failed to load devices: ${error.message ?: "unknown error"}"
            }
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

        val points = validWifi.map { GeoPoint(it.latitude, it.longitude) } +
            validBluetooth.map { GeoPoint(it.latitude, it.longitude) }

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
            position = GeoPoint(latitude, longitude)
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
            this.color = Color.WHITE
        }
        canvas.drawCircle(size / 2f, size / 2f, radius, fill)
        canvas.drawCircle(size / 2f, size / 2f, radius, border)
        return BitmapDrawable(resources, bitmap)
    }

    private fun hasValidLocation(latitude: Double, longitude: Double): Boolean {
        if (latitude == 0.0 && longitude == 0.0) return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun createMenuButton(): TextView {
        return TextView(this).apply {
            text = "Menu"
            textSize = 14f
            setTextColor(0xff1565C0.toInt())
            setBackgroundColor(Color.WHITE)
            gravity = Gravity.CENTER
            setOnClickListener { drawerOverlay.visibility = View.VISIBLE }
        }
    }

    private fun createDrawerOverlay(): FrameLayout {
        return FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(0x66000000)
            setOnClickListener { visibility = View.GONE }
            addView(
                LinearLayout(this@OsmMapActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.WHITE)
                    setPadding(dp(16), dp(16), dp(16), dp(16))
                    isClickable = true
                    addView(drawerTitle())
                    addView(drawerItem("Scanner", "Scanner", selected = false))
                    addView(drawerItem("Devices", "DeviceList", selected = false))
                    addView(drawerItem("Statistics", "Statistics", selected = false))
                    addView(drawerItem("Map", "Map", selected = true))
                    addView(drawerItem("Settings", "Settings", selected = false))
                },
                FrameLayout.LayoutParams(
                    dp(280),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.START
                )
            )
        }
    }

    private fun drawerTitle(): TextView {
        return TextView(this).apply {
            text = "ScanApp"
            textSize = 20f
            setTextColor(0xff1C1B1F.toInt())
            setPadding(0, 0, 0, dp(16))
        }
    }

    private fun drawerItem(label: String, pageName: String, selected: Boolean): TextView {
        return TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(if (selected) 0xff001D36.toInt() else 0xff1C1B1F.toInt())
            setBackgroundColor(if (selected) 0xffD1E4FF.toInt() else Color.TRANSPARENT)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply {
                bottomMargin = dp(4)
            }
            setOnClickListener { navigateFromDrawer(pageName) }
        }
    }

    private fun navigateFromDrawer(pageName: String) {
        drawerOverlay.visibility = View.GONE
        if (pageName == "Map") return
        KuiklyRenderActivity.start(this, pageName)
        finish()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
        const val EXTRA_TITLE = "extra_title"
    }
}
