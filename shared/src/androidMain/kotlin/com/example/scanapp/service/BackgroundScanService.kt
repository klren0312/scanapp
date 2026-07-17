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
import com.example.scanapp.logging.CrashLogger
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import com.example.scanapp.platform.AndroidBluetoothScanner
import com.example.scanapp.platform.AndroidCellScanner
import com.example.scanapp.platform.AndroidLocationTracker
import com.example.scanapp.platform.AndroidWifiScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.ArrayDeque
import kotlin.coroutines.resume

class BackgroundScanService : Service() {

    private lateinit var wifiScanner: AndroidWifiScanner
    private lateinit var bluetoothScanner: AndroidBluetoothScanner
    private lateinit var cellScanner: AndroidCellScanner
    private lateinit var locationTracker: AndroidLocationTracker
    private val bluetoothDao by lazy { BluetoothScanDao(DatabaseFactory.getDatabase()) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanningJob: Job? = null
    private var bluetoothStartJob: Job? = null
    private var bluetoothBatchJob: Job? = null
    private val bluetoothQueueLock = Any()
    private val pendingBluetoothRecords = ArrayDeque<BluetoothScanRecord>()
    private val bluetoothFlushSignal = Channel<Unit>(Channel.CONFLATED)
    private var droppedBluetoothRecords = 0
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
        wifiScanner.stopScan()
        stopBluetoothScanningAndFlush()
        scope.cancel()
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
        startBluetoothBatchWriter()
        bluetoothStartJob = scope.launch {
            delay(500L)
            try {
                bluetoothScanner.startScan(
                    callback = ::enqueueBluetoothRecord,
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

    private fun startBluetoothBatchWriter() {
        if (bluetoothBatchJob?.isActive == true) return
        bluetoothBatchJob = scope.launch {
            while (isActive) {
                withTimeoutOrNull(BLUETOOTH_FLUSH_INTERVAL_MS) {
                    bluetoothFlushSignal.receive()
                }
                logDroppedBluetoothRecords()
                if (!flushPendingBluetoothRecords()) {
                    delay(BLUETOOTH_RETRY_DELAY_MS)
                }
            }
        }
    }

    private fun enqueueBluetoothRecord(record: BluetoothScanRecord) {
        val shouldFlush = synchronized(bluetoothQueueLock) {
            if (pendingBluetoothRecords.size >= MAX_PENDING_BLUETOOTH_RECORDS) {
                pendingBluetoothRecords.removeFirst()
                droppedBluetoothRecords++
            }
            pendingBluetoothRecords.addLast(record)
            pendingBluetoothRecords.size >= BLUETOOTH_BATCH_SIZE
        }
        if (shouldFlush) bluetoothFlushSignal.trySend(Unit)
    }

    private suspend fun flushPendingBluetoothRecords(): Boolean {
        while (true) {
            val batch = takeBluetoothBatch()
            if (batch.isEmpty()) return true
            if (!persistBluetoothBatch(batch)) return false
        }
    }

    private fun takeBluetoothBatch(): List<BluetoothScanRecord> {
        return synchronized(bluetoothQueueLock) {
            val batchSize = minOf(BLUETOOTH_BATCH_SIZE, pendingBluetoothRecords.size)
            List(batchSize) { pendingBluetoothRecords.removeFirst() }
        }
    }

    private suspend fun persistBluetoothBatch(batch: List<BluetoothScanRecord>): Boolean {
        return try {
            withContext(NonCancellable + Dispatchers.IO) {
                val location = locationTracker.getCurrentLocation()
                val latitude = location?.latitude ?: 0.0
                val longitude = location?.longitude ?: 0.0
                val recordsWithLocation = batch.map { record ->
                    record.copy(latitude = latitude, longitude = longitude)
                }
                bluetoothDao.insertBatch(recordsWithLocation)
            }
            true
        } catch (error: Exception) {
            requeueBluetoothBatch(batch)
            val msg = "Bluetooth batch save error (${batch.size} records): ${error.message}"
            android.util.Log.e(TAG, msg)
            CrashLogger.log(TAG, msg)
            false
        }
    }

    private fun requeueBluetoothBatch(batch: List<BluetoothScanRecord>) {
        synchronized(bluetoothQueueLock) {
            batch.asReversed().forEach { pendingBluetoothRecords.addFirst(it) }
            while (pendingBluetoothRecords.size > MAX_PENDING_BLUETOOTH_RECORDS) {
                pendingBluetoothRecords.removeLast()
                droppedBluetoothRecords++
            }
        }
    }

    private fun logDroppedBluetoothRecords() {
        val dropped = synchronized(bluetoothQueueLock) {
            droppedBluetoothRecords.also { droppedBluetoothRecords = 0 }
        }
        if (dropped == 0) return
        val msg = "Dropped $dropped Bluetooth scan records because the batch queue was full"
        android.util.Log.w(TAG, msg)
        CrashLogger.log(TAG, msg)
    }

    private fun stopBluetoothScanningAndFlush() {
        // The queue is bounded, so shutdown waits for a finite amount of pending work.
        runBlocking {
            bluetoothStartJob?.cancel()
            bluetoothStartJob?.join()
            bluetoothStartJob = null
            bluetoothScanner.stopScan()
            bluetoothBatchJob?.cancel()
            bluetoothBatchJob?.join()
            bluetoothBatchJob = null
            logDroppedBluetoothRecords()
            flushPendingBluetoothRecords()
            logDroppedBluetoothRecords()
        }
        bluetoothFlushSignal.close()
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
        private const val TAG = "BackgroundScanService"
        private const val BLUETOOTH_BATCH_SIZE = 100
        private const val MAX_PENDING_BLUETOOTH_RECORDS = 500
        private const val BLUETOOTH_FLUSH_INTERVAL_MS = 750L
        private const val BLUETOOTH_RETRY_DELAY_MS = 2_000L

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
