package com.example.scanapp.service

import android.content.Context
import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.CellScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.PendingUploadDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import com.example.scanapp.platform.AndroidBluetoothScanner
import com.example.scanapp.platform.AndroidCellScanner
import com.example.scanapp.platform.AndroidWifiScanner
import com.example.scanapp.service.UploadSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class AndroidScannerService(
    private val context: Context,
    private val wifiDao: WifiScanDao,
    private val bluetoothDao: BluetoothScanDao,
    private val locationService: LocationService
) : ScannerService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val wifiScanner = AndroidWifiScanner(context)
    private val bluetoothScanner = AndroidBluetoothScanner(context)
    private val cellScanner = AndroidCellScanner(context)
    private val bluetoothStateMutex = Mutex()

    private val uploadService = UploadService(object : UploadTransportLike {
        override suspend fun postJson(url: String, token: String, body: String): Boolean =
            UploadTransport.postJson(url, token, body)
    })
    private val uploaderId: String get() = UploadSettings.uploaderId.ifBlank { "default" }

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning

    private val _wifiDevices = MutableStateFlow<List<WifiScanRecord>>(emptyList())
    override val wifiDevices: StateFlow<List<WifiScanRecord>> = _wifiDevices

    private val _bluetoothDevices = MutableStateFlow<List<BluetoothScanRecord>>(emptyList())
    override val bluetoothDevices: StateFlow<List<BluetoothScanRecord>> = _bluetoothDevices

    private var scanJob: Job? = null

    override suspend fun startWifiScan() {
        wifiScanner.startScan()
        val devices = wifiScanner.scanWifiNetworks()
        _wifiDevices.value = devices
        val location = locationService.getCurrentLocation()
        val devicesWithLocation = devices.map { device ->
            device.copy(
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0
            )
        }
        wifiDao.insertBatch(devicesWithLocation)
    }

    override suspend fun stopWifiScan() {
        // WiFi扫描为单次操作，无需停止
    }

    override suspend fun startBluetoothScan() {
        if (!bluetoothScanner.isBluetoothEnabled()) {
            // 蓝牙未开启，记录错误
            return
        }
        bluetoothScanner.startScan(
            callback = ::handleBluetoothResult,
            onError = { error ->
                // 蓝牙扫描失败，可在此上报
                android.util.Log.e("AndroidScannerService", error)
            }
        )
    }

    override suspend fun stopBluetoothScan() {
        bluetoothScanner.stopScan()
    }

    override suspend fun startAllScans() {
        _isScanning.value = true

        // 启动蓝牙扫描（持续监听）
        bluetoothScanner.startScan(
            callback = ::handleBluetoothResult,
            onError = { error ->
                android.util.Log.e("AndroidScannerService", error)
            }
        )

        scanJob = scope.launch {
            while (true) {
                try {
                    // Flush any pending uploads from prior scan cycles first.
                    flushPendingUploads()

                    // 扫描WiFi
                    wifiScanner.startScan()
                    val wifiDevices = wifiScanner.scanWifiNetworks()
                    _wifiDevices.value = wifiDevices

                    // 获取当前位置（一次获取，多次使用）
                    val location = locationService.getCurrentLocation()

                    // 批量保存WiFi到数据库
                    val devicesWithLocation = wifiDevices.map { device ->
                        device.copy(
                            latitude = location?.latitude ?: 0.0,
                            longitude = location?.longitude ?: 0.0
                        )
                    }
                    wifiDao.insertBatch(devicesWithLocation)
                    enqueueUpload(
                        ScanBatch(
                            uploaderId = uploaderId,
                            wifi = devicesWithLocation.map {
                                WifiSighting(it.bssid, it.ssid, it.signalStrength, it.frequency, it.latitude, it.longitude, it.timestamp)
                            },
                            bluetooth = emptyList()
                        )
                    )

                    val cellResults = try {
                        cellScanner.scanCellInfo()
                    } catch (e: Exception) {
                        android.util.Log.e("AndroidScannerService", "Cell scan error: ${e.message}")
                        emptyList()
                    }
                    if (cellResults.isNotEmpty()) {
                        val cellDao = CellScanDao(DatabaseFactory.getDatabase())
                        val cellWithLocation = cellResults.map { record ->
                            record.copy(
                                latitude = location?.latitude ?: 0.0,
                                longitude = location?.longitude ?: 0.0
                            )
                        }
                        cellDao.insertBatch(cellWithLocation)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AndroidScannerService", "WiFi scan/save error: ${e.message}")
                }

                delay(5000) // 每5秒扫描一次
            }
        }
    }

    override suspend fun stopAllScans() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
        bluetoothScanner.stopScan()
    }

    private fun handleBluetoothResult(record: BluetoothScanRecord) {
        scope.launch {
            try {
                val location = locationService.getCurrentLocation()
                val recordWithLocation = record.copy(
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0
                )
                bluetoothDao.insertOrUpdate(recordWithLocation)
                val storedRecord = bluetoothDao.getRecordByAddress(recordWithLocation.address)
                    ?: recordWithLocation
                bluetoothStateMutex.withLock {
                    val current = _bluetoothDevices.value
                    val existingIndex = current.indexOfFirst { it.address == storedRecord.address }
                    if (existingIndex < 0) {
                        _bluetoothDevices.value = listOf(storedRecord) + current
                    } else if (storedRecord.count >= current[existingIndex].count) {
                        _bluetoothDevices.value = current.toMutableList().apply {
                            removeAt(existingIndex)
                            add(0, storedRecord)
                        }
                    }
                }
                enqueueUpload(
                    ScanBatch(
                        uploaderId = uploaderId,
                        wifi = emptyList(),
                        bluetooth = listOf(
                            BtSighting(
                                storedRecord.address,
                                storedRecord.name,
                                storedRecord.rssi,
                                storedRecord.deviceType,
                                storedRecord.latitude,
                                storedRecord.longitude,
                                storedRecord.timestamp
                            )
                        )
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("AndroidScannerService", "Bluetooth save error: ${e.message}")
            }
        }
    }

    private suspend fun enqueueUpload(batch: ScanBatch) {
        val db = DatabaseFactory.getDatabase()
        PendingUploadDao(db).enqueue(uploadService.buildPayload(batch), System.currentTimeMillis())
        flushPendingUploads()
    }

    private suspend fun flushPendingUploads() {
        if (!UploadSettings.uploadEnabled) return
        val url = UploadSettings.serverUrl
        val token = UploadSettings.uploadToken
        if (url.isBlank() || token.isBlank()) return
        val db = DatabaseFactory.getDatabase()
        val dao = PendingUploadDao(db)
        if (dao.count() > 500) {
            val oldest = dao.peekOldest(500)
            dao.deleteUpTo(oldest.last().id)
        }
        val rows = dao.peekOldest(50)
        if (rows.isEmpty()) return
        // Upload in order, stop at the first failure so a later success can't sweep a failed row
        // out of the queue. Only delete contiguous leading successes.
        var maxId = -1L
        for (row in rows) {
            val batch = try {
                Json.decodeFromString<ScanBatch>(row.payload)
            } catch (e: Exception) {
                maxId = row.id
                continue
            }
            val ok = uploadService.tryUpload(url, token, batch)
            if (ok) {
                maxId = row.id
            } else {
                break
            }
        }
        if (maxId > 0) dao.deleteUpTo(maxId)
    }
}
