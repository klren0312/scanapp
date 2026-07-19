package com.example.scanapp.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface UploadTransportLike {
    suspend fun postJson(url: String, token: String, body: String): Boolean
}

@Serializable
data class WifiSighting(
    val bssid: String, val ssid: String, val signal: Int,
    val frequency: Int, val lat: Double, val lng: Double, val timestamp: Long
)

@Serializable
data class BtSighting(
    val address: String, val name: String, val rssi: Int,
    val deviceType: String, val lat: Double, val lng: Double, val timestamp: Long
)

@Serializable
data class ScanBatch(
    val uploaderId: String,
    val wifi: List<WifiSighting>,
    val bluetooth: List<BtSighting>
)

class UploadService(private val transport: UploadTransportLike) {

    fun buildPayload(batch: ScanBatch): String {
        val wifi = batch.wifi.filter { it.lat != 0.0 || it.lng != 0.0 }
        val bt = batch.bluetooth.filter { it.lat != 0.0 || it.lng != 0.0 }
        return Json.encodeToString(ScanBatch(batch.uploaderId, wifi, bt))
    }

    suspend fun tryUpload(serverUrl: String, token: String, batch: ScanBatch): Boolean {
        if (serverUrl.isBlank() || token.isBlank()) return false
        return transport.postJson(serverUrl, token, buildPayload(batch))
    }
}
