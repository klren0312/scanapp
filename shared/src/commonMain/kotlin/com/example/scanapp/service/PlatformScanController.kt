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
enum class CellScanReadiness {
    READY,
    MISSING_PERMISSION,
    UNSUPPORTED
}

expect fun getCellScanReadiness(): CellScanReadiness
// Re-requests the location permission needed for cell (base station) scanning.
// Only Android shows the OS permission dialog; iOS/ohos are no-ops.
expect fun requestCellScanPermission()