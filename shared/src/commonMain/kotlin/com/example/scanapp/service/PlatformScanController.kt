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
// Shared, platform-neutral explanation for why the Cell (base station) count may be
// zero. Returns an empty string when the count is positive or scanning is irrelevant.
fun cellReadinessHint(cellCount: Long): String {
    if (cellCount > 0L) return ""
    return when (getCellScanReadiness()) {
        CellScanReadiness.MISSING_PERMISSION ->
            "Cell (base station) needs location permission. Grant it, then restart scanning."
        CellScanReadiness.UNSUPPORTED ->
            "Cell (base station) scanning is not available on this platform."
        CellScanReadiness.READY ->
            "No cell towers detected yet. Move outdoors or wait a few cycles."
    }
}