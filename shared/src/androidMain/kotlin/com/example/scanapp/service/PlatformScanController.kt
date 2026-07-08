package com.example.scanapp.service

import com.example.scanapp.database.AndroidDatabaseDriver

actual object PlatformScanController {
    actual fun startBackgroundScanning(): ScanControlResult {
        return runCatching {
            BackgroundScanService.start(AndroidDatabaseDriver.requireContext())
            ScanControlResult(true, "Scanning started")
        }.getOrElse {
            ScanControlResult(false, "Start failed: ${it.message ?: it::class.simpleName}")
        }
    }

    actual fun stopBackgroundScanning(): ScanControlResult {
        return runCatching {
            BackgroundScanService.stop(AndroidDatabaseDriver.requireContext())
            ScanControlResult(true, "Scanning stopped")
        }.getOrElse {
            ScanControlResult(false, "Stop failed: ${it.message ?: it::class.simpleName}")
        }
    }
}
