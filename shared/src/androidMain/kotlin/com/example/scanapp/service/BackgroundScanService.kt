package com.example.scanapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.CellScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.LocationDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.platform.AndroidBluetoothScanner
import com.example.scanapp.platform.AndroidCellScanner
import com.example.scanapp.platform.AndroidLocationTracker
import com.example.scanapp.platform.AndroidWifiScanner
import com.example.scanapp.models.WifiScanRecord
import com.example.scanapp.logging.CrashLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class BackgroundScanService : Service() {

    private lateinit var wifiScanner: AndroidWifiScanner
    private lateinit var bluetoothScanner: AndroidBluetoothScanner
    private lateinit var cellScanner: AndroidCellScanner
    private lateinit var locationTracker: AndroidLocationTracker
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanningJob: Job? = null
    private val scanInterval = 30_000L

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        wifiScanner = AndroidWifiScanner(this)
        bluetoothScanner = AndroidBluetoothScanner(this)
        cellScanner = AndroidCellScanner(this)
        locationTracker = AndroidLocationTracker(this)

        createNotificationChannel()
        startForeground(1, createNotification())

        locationTracker.startTracking()
        startScanning()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        scanningJob?.cancel()
        scope.cancel()
        wifiScanner.stopScan()
        bluetoothScanner.stopScan()
        locationTracker.stopTracking()
        stopForeground(true)
        super.onDestroy()
    }

    private fun startScanning() {
        if (!bluetoothScanner.isBluetoothEnabled()) {
            val msg = "Bluetooth is disabled. Bluetooth scanning will not run until it is enabled."
            android.util.Log.e("BackgroundScanService", msg)
            CrashLogger.log("BackgroundScanService", msg)
        }
        // Give the foreground service/BLE stack a moment to settle before registering
        // the scan client; this avoids SCAN_FAILED_APPLICATION_REGISTRATION_FAILED on
        // devices that are sensitive to immediate startScan after service creation.
        scope.launch {
            delay(500L)
            try {
                bluetoothScanner.startScan(
                    callback = { record ->
                        scope.launch {
                            try {
                                val location = locationTracker.getCurrentLocation()
                                val recordWithLocation = record.copy(
                                    latitude = location?.latitude ?: 0.0,
                                    longitude = location?.longitude ?: 0.0
                                )
                                val database = DatabaseFactory.getDatabase()
                                val bluetoothDao = BluetoothScanDao(database)
                                bluetoothDao.insertOrUpdate(recordWithLocation)
                            } catch (e: Exception) {
                                val msg = "Bluetooth save error: ${e.message}"
                                android.util.Log.e("BackgroundScanService", msg)
                                CrashLogger.log("BackgroundScanService", msg)
                            }
                        }
                    },
                    onError = { error ->
                        val msg = "Bluetooth scan error: $error"
                        android.util.Log.e("BackgroundScanService", msg)
                        CrashLogger.log("BackgroundScanService", msg)
                    }
                )
            } catch (e: Exception) {
                // Never let a BLE failure abort onCreate — cell scanning must still run.
                val msg = "Bluetooth startScan threw, continuing without BLE: ${e.message}"
                android.util.Log.e("BackgroundScanService", msg)
                CrashLogger.log("BackgroundScanService", msg)
            }
        }

        scanningJob = scope.launch {
            while (isActive) {
                val location = locationTracker.getCurrentLocation()

                val wifiResults = withTimeoutOrNull(15_000L) {
                    suspendCancellableCoroutine<List<WifiScanRecord>> { cont ->
                        wifiScanner.startScan { results ->
                            if (cont.isActive) cont.resume(results)
                        }
                        cont.invokeOnCancellation {
                            wifiScanner.stopScan()
                        }
                    }
                } ?: emptyList()

                val cellResults = try {
                    cellScanner.scanCellInfo()
                } catch (e: Exception) {
                    val msg = "Cell scan error: ${e.message}"
                    android.util.Log.e("BackgroundScanService", msg)
                    CrashLogger.log("BackgroundScanService", msg)
                    emptyList()
                }

                try {
                    val database = DatabaseFactory.getDatabase()
                    val wifiDao = WifiScanDao(database)
                    val cellDao = CellScanDao(database)
                    val cellLat = location?.latitude ?: 0.0
                    val cellLon = location?.longitude ?: 0.0

                    val recordsWithLocation = wifiResults.map { record ->
                        record.copy(
                            latitude = cellLat,
                            longitude = cellLon
                        )
                    }
                    if (recordsWithLocation.isNotEmpty()) {
                        wifiDao.insertBatch(recordsWithLocation)
                    }

                    if (cellResults.isNotEmpty()) {
                        val cellWithLocation = cellResults.map { record ->
                            record.copy(latitude = cellLat, longitude = cellLon)
                        }
                        cellDao.insertBatch(cellWithLocation)
                    }

                    location?.let {
                        val locationDao = LocationDao(database)
                        locationDao.insert(it)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BackgroundScanService", "WiFi/Cell save error: ${e.message}")
                }

                delay(scanInterval)
            }
        }
    }

    private fun createNotificationChannel() {
        val channelId = "scan_service_channel"
        val channelName = "扫描服务"
        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = "后台扫描服务通知"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val channelId = "scan_service_channel"

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("WiFi/蓝牙扫描器")
            .setContentText("正在后台扫描...")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, BackgroundScanService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, BackgroundScanService::class.java)
            context.stopService(intent)
        }
    }
}
