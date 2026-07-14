package com.example.scanapp.service

import android.content.Context
import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import com.example.scanapp.platform.AndroidBluetoothScanner
import com.example.scanapp.platform.AndroidWifiScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AndroidScannerService(
    private val context: Context,
    private val wifiDao: WifiScanDao,
    private val bluetoothDao: BluetoothScanDao,
    private val locationService: LocationService
) : ScannerService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val wifiScanner = AndroidWifiScanner(context)
    private val bluetoothScanner = AndroidBluetoothScanner(context)

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
            callback = { record ->
                scope.launch {
                    val location = locationService.getCurrentLocation()
                    val recordWithLocation = record.copy(
                        latitude = location?.latitude ?: 0.0,
                        longitude = location?.longitude ?: 0.0
                    )
                    bluetoothDao.insertBatch(listOf(recordWithLocation))
                    _bluetoothDevices.value = bluetoothDao.getAllRecords()
                }
            },
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
            callback = { record ->
                scope.launch {
                    val location = locationService.getCurrentLocation()
                    val recordWithLocation = record.copy(
                        latitude = location?.latitude ?: 0.0,
                        longitude = location?.longitude ?: 0.0
                    )
                    bluetoothDao.insertBatch(listOf(recordWithLocation))
                    _bluetoothDevices.value = bluetoothDao.getAllRecords()
                }
            },
            onError = { error ->
                android.util.Log.e("AndroidScannerService", error)
            }
        )

        scanJob = scope.launch {
            while (true) {
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
}
