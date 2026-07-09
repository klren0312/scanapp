package com.example.scanapp

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.LocationDao
import com.example.scanapp.models.LocationRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class OsmMapActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var mapView: MapView
    private lateinit var statusView: TextView
    private lateinit var drawerOverlay: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Map"

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
            text = "Loading locations..."
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

        loadLocations()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
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

    private fun loadLocations() {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    LocationDao(DatabaseFactory.getDatabase()).getAllRecords()
                }
            }.onSuccess { records ->
                renderLocations(records)
            }.onFailure { error ->
                statusView.text = "Failed to load locations: ${error.message ?: "unknown error"}"
            }
        }
    }

    private fun renderLocations(records: List<LocationRecord>) {
        val validRecords = records.filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }

        validRecords.forEachIndexed { index, record ->
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = GeoPoint(record.latitude, record.longitude)
                    title = "Location #${index + 1}"
                    snippet = "Accuracy: ${formatOneDecimal(record.accuracy.toDouble())} m"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )
        }

        if (validRecords.isNotEmpty()) {
            val latest = validRecords.last()
            mapView.controller.setZoom(16.0)
            mapView.controller.setCenter(GeoPoint(latest.latitude, latest.longitude))
            statusView.text = "OpenStreetMap locations: ${validRecords.size}"
        } else {
            mapView.controller.setZoom(3.0)
            mapView.controller.setCenter(GeoPoint(0.0, 0.0))
            statusView.text = "No location records"
        }

        mapView.invalidate()
    }

    private fun formatOneDecimal(value: Double): String {
        val tenths = kotlin.math.round(value * 10.0).toLong()
        val sign = if (tenths < 0) "-" else ""
        val absolute = kotlin.math.abs(tenths)
        return "$sign${absolute / 10}.${absolute % 10}"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
