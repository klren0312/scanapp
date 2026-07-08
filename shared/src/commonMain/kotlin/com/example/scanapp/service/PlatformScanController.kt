package com.example.scanapp.service

expect object PlatformScanController {
    fun startBackgroundScanning(): ScanControlResult
    fun stopBackgroundScanning(): ScanControlResult
}

data class ScanControlResult(
    val success: Boolean,
    val message: String
)
