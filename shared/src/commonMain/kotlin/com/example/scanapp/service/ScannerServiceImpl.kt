package com.example.scanapp.service

import com.example.scanapp.database.BluetoothScanDao
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

class ScannerServiceImpl(
    private val wifiDao: WifiScanDao,
    private val bluetoothDao: BluetoothScanDao,
    private val locationService: LocationService,
    private val onScanWifi: suspend () -> List<WifiScanRecord>,
    private val onScanBluetooth: suspend () -> List<BluetoothScanRecord>,
    private val scanIntervalMs: Long = 5000L
) : ScannerService {

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
    }
}
