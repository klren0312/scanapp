package com.example.scanapp

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Map"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
}
