package com.example.scanapp.platform

import android.content.Context
import android.net.wifi.WifiManager
import com.example.scanapp.models.WifiScanRecord

class AndroidWifiScanner(private val context: Context) {
    
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    
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
                latitude = 0.0, // 将由LocationTracker提供
                longitude = 0.0 // 将由LocationTracker提供
            )
        }
    }
    
    fun startScan() {
        wifiManager.startScan()
    }
}