package com.example.scanapp.models

import kotlinx.serialization.Serializable

@Serializable
data class WifiScanRecord(
    val id: Long = 0,
    val ssid: String,
    val bssid: String,
    val signalStrength: Int,
    val frequency: Int,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val count: Int = 1
)