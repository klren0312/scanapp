package com.example.scanapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.platform.AndroidBluetoothScanner
import com.example.scanapp.platform.AndroidLocationTracker
import com.example.scanapp.platform.AndroidWifiScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackgroundScanService : Service() {

    private lateinit var wifiScanner: AndroidWifiScanner
    private lateinit var bluetoothScanner: AndroidBluetoothScanner
    private lateinit var locationTracker: AndroidLocationTracker
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        wifiScanner = AndroidWifiScanner(this)
        bluetoothScanner = AndroidBluetoothScanner(this)
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
        super.onDestroy()

        locationTracker.stopTracking()
        bluetoothScanner.stopScan()
        stopForeground(true)
        stopSelf()
    }

    private fun startScanning() {
        wifiScanner.startScan()

        bluetoothScanner.startScan { record ->
            scope.launch {
                val location = locationTracker.getCurrentLocation()
                val recordWithLocation = record.copy(
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0
                )

                val database = DatabaseFactory.getDatabase()
                val bluetoothDao = BluetoothScanDao(database)
                bluetoothDao.insertBatch(listOf(recordWithLocation))
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
