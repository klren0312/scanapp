package com.example.scanapp.models

import kotlinx.serialization.Serializable

@Serializable
data class BluetoothScanRecord(
    val id: Long = 0,
    val name: String,
    val address: String,
    val rssi: Int,
    val deviceType: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val count: Int = 1
)