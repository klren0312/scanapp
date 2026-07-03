package com.example.scanapp.service

import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import kotlinx.coroutines.flow.StateFlow

interface ScannerService {
    val isScanning: StateFlow<Boolean>
    val wifiDevices: StateFlow<List<WifiScanRecord>>
    val bluetoothDevices: StateFlow<List<BluetoothScanRecord>>

    suspend fun startWifiScan()
    suspend fun stopWifiScan()
    suspend fun startBluetoothScan()
    suspend fun stopBluetoothScan()
    suspend fun startAllScans()
    suspend fun stopAllScans()
}
