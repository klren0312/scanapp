package com.example.scanapp.service

actual object PlatformScanController {
    actual fun startBackgroundScanning(): ScanControlResult {
        return ScanControlResult(false, "Scanning is not available on iOS yet")
    }

    actual fun stopBackgroundScanning(): ScanControlResult {
        return ScanControlResult(true, "Scanning stopped")
    }
}
