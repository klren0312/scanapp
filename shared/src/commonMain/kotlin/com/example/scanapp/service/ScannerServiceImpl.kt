package com.example.scanapp.service

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.DatabaseFactory
import com.example.scanapp.database.PendingUploadDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ScannerServiceImpl(
    private val wifiDao: WifiScanDao,
    private val bluetoothDao: BluetoothScanDao,
    private val locationService: LocationService,
    private val onScanWifi: suspend () -> List<WifiScanRecord>,
    private val onScanBluetooth: suspend () -> List<BluetoothScanRecord>,
    private val scanIntervalMs: Long = 5000L,
    private val uploaderId: String = "default",
    private val serverUrlProvider: () -> String = { "" },
    private val uploadTokenProvider: () -> String = { "" },
    private val uploadEnabledProvider: () -> Boolean = { false }
) : ScannerService {

    private val uploadService = UploadService(object : UploadTransportLike {
        override suspend fun postJson(url: String, token: String, body: String): Boolean =
            UploadTransport.postJson(url, token, body)
    })

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning

    private val _wifiDevices = MutableStateFlow<List<WifiScanRecord>>(emptyList())
    override val wifiDevices: StateFlow<List<WifiScanRecord>> = _wifiDevices

    private val _bluetoothDevices = MutableStateFlow<List<BluetoothScanRecord>>(emptyList())
    override val bluetoothDevices: StateFlow<List<BluetoothScanRecord>> = _bluetoothDevices

    private var scanJob: Job? = null

    override suspend fun startWifiScan() {
        val devices = onScanWifi()
        _wifiDevices.value = devices
        saveWifiDevices(devices)
    }

    override suspend fun stopWifiScan() {
        // WiFi扫描为单次操作，无需停止
    }

    override suspend fun startBluetoothScan() {
        val devices = onScanBluetooth()
        _bluetoothDevices.value = devices
        saveBluetoothDevices(devices)
    }

    override suspend fun stopBluetoothScan() {
        // 蓝牙扫描为单次操作，无需停止
    }

    override suspend fun startAllScans() {
        _isScanning.value = true
        scanJob = scope.launch {
            while (true) {
                try {
                    flushPendingUploads()

                    val wifiDevices = onScanWifi()
                    _wifiDevices.value = wifiDevices
                    saveWifiDevices(wifiDevices)

                    val bluetoothDevices = onScanBluetooth()
                    _bluetoothDevices.value = bluetoothDevices
                    saveBluetoothDevices(bluetoothDevices)
                } catch (e: Exception) {
                    // Log error but continue scanning
                    e.printStackTrace()
                }
                delay(scanIntervalMs)
            }
        }
    }

    override suspend fun stopAllScans() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
    }

    private suspend fun saveWifiDevices(devices: List<WifiScanRecord>) {
        val location = locationService.getCurrentLocation()
        val devicesWithLocation = devices.map { device ->
            device.copy(
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0
            )
        }
        wifiDao.insertBatch(devicesWithLocation)
        val batch = ScanBatch(
            uploaderId = uploaderId,
            wifi = devicesWithLocation.map {
                WifiSighting(it.bssid, it.ssid, it.signalStrength, it.frequency, it.latitude, it.longitude, it.timestamp)
            },
            bluetooth = emptyList()
        )
        enqueueUpload(batch)
    }

    private suspend fun saveBluetoothDevices(devices: List<BluetoothScanRecord>) {
        val location = locationService.getCurrentLocation()
        val devicesWithLocation = devices.map { device ->
            device.copy(
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0
            )
        }
        bluetoothDao.insertBatch(devicesWithLocation)
        val batch = ScanBatch(
            uploaderId = uploaderId,
            wifi = emptyList(),
            bluetooth = devicesWithLocation.map {
                BtSighting(it.address, it.name, it.rssi, it.deviceType, it.latitude, it.longitude, it.timestamp)
            }
        )
        enqueueUpload(batch)
    }

    private suspend fun enqueueUpload(batch: ScanBatch) {
        val db = DatabaseFactory.getDatabase()
        PendingUploadDao(db).enqueue(uploadService.buildPayload(batch), System.currentTimeMillis())
        flushPendingUploads()
    }

    private suspend fun flushPendingUploads() {
        if (!uploadEnabledProvider()) return
        val url = serverUrlProvider()
        val token = uploadTokenProvider()
        if (url.isBlank() || token.isBlank()) return
        val db = DatabaseFactory.getDatabase()
        val dao = PendingUploadDao(db)
        if (dao.count() > 500) {
            val oldest = dao.peekOldest(500)
            dao.deleteUpTo(oldest.last().id)
        }
        val rows = dao.peekOldest(50)
        if (rows.isEmpty()) return
        // Upload in order, stop at the first failure so a later success can't sweep a failed
        // row out of the queue (deleteUpTo would remove everything up to the last success id,
        // including the failed row). Only delete contiguous leading successes.
        var maxId = -1L
        for (row in rows) {
            val batch = try {
                Json.decodeFromString<ScanBatch>(row.payload)
            } catch (e: Exception) {
                // Unparseable payload (e.g. schema drift after an upgrade): drop it and continue.
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
