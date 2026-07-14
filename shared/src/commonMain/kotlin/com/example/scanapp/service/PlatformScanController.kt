package com.example.scanapp.service

expect object PlatformScanController {
    fun startBackgroundScanning(): ScanControlResult
    fun stopBackgroundScanning(): ScanControlResult
    fun isBluetoothEnabled(): Boolean
    fun requestEnableBluetooth(onEnabled: () -> Unit)
    fun openDeviceMap(latitude: Double, longitude: Double, title: String)
}

data class ScanControlResult(
    val success: Boolean,
    val message: String
)
