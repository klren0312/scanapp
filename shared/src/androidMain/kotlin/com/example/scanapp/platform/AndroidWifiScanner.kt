package com.example.scanapp.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import com.example.scanapp.models.WifiScanRecord

class AndroidWifiScanner(private val context: Context) {

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var scanReceiver: BroadcastReceiver? = null

    fun scanWifiNetworks(): List<WifiScanRecord> {
        val scanResults = wifiManager.scanResults
        val currentTime = System.currentTimeMillis()

        return scanResults.map { scanResult ->
            WifiScanRecord(
                ssid = scanResult.SSID ?: "Unknown",
                bssid = scanResult.BSSID ?: "",
                signalStrength = scanResult.level,
                frequency = scanResult.frequency,
                timestamp = currentTime,
                latitude = 0.0,
                longitude = 0.0
            )
        }
    }

    fun startScan(callback: (List<WifiScanRecord>) -> Unit) {
        stopScan()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    context.unregisterReceiver(this)
                    if (scanReceiver == this) scanReceiver = null
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    callback(if (success) scanWifiNetworks() else emptyList())
                }
            }
        }
        scanReceiver = receiver
        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifiManager.startScan()
    }

    fun stopScan() {
        scanReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: IllegalArgumentException) {}
        }
        scanReceiver = null
    }

    @Deprecated("Use startScan(callback) for async scan results")
    fun startScan() {
        wifiManager.startScan()
    }
}
