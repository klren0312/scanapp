package com.example.scanapp.service

actual object PlatformScanController {
    actual fun startBackgroundScanning(): ScanControlResult {
        return ScanControlResult(false, "Scanning is not available on HarmonyOS yet")
    }

    actual fun stopBackgroundScanning(): ScanControlResult {
        return ScanControlResult(true, "Scanning stopped")
    }

    actual fun isBluetoothEnabled(): Boolean = true

    actual fun requestEnableBluetooth(onEnabled: () -> Unit) {}

    actual fun openDeviceMap(latitude: Double, longitude: Double, title: String) {}
}
