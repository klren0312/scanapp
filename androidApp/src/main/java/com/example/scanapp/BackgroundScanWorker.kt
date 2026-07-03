package com.example.scanapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.service.AndroidLocationService
import com.example.scanapp.service.AndroidScannerService
import kotlinx.coroutines.delay

class BackgroundScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = DatabaseFactory.getDatabase()
            val wifiDao = WifiScanDao(database)
            val bluetoothDao = BluetoothScanDao(database)
            val locationService = AndroidLocationService(applicationContext)

            val scannerService = AndroidScannerService(
                applicationContext,
                wifiDao,
                bluetoothDao,
                locationService
            )

            locationService.startTracking()
            scannerService.startAllScans()

            delay(30000)

            scannerService.stopAllScans()
            locationService.stopTracking()

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
