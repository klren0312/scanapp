package com.example.scanapp.platform

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.example.scanapp.models.BluetoothScanRecord

class AndroidBluetoothScanner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bleScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var onDeviceFound: ((BluetoothScanRecord) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun startScan(
        callback: (BluetoothScanRecord) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        onDeviceFound = callback
        this.onError = onError

        if (!isBluetoothEnabled()) {
            onError("Bluetooth is disabled. Please enable Bluetooth to scan.")
            return
        }

        bleScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bleScanner == null) {
            onError("BluetoothLeScanner is unavailable on this device.")
            return
        }

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val record = createScanRecord(device, result.rssi)
                onDeviceFound?.invoke(record)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { result ->
                    val device = result.device
                    val record = createScanRecord(device, result.rssi)
                    onDeviceFound?.invoke(record)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                onError(mapErrorCode(errorCode))
            }
        }

        bleScanner?.startScan(scanCallback)
    }

    fun stopScan() {
        scanCallback?.let { bleScanner?.stopScan(it) }
        scanCallback = null
        onDeviceFound = null
        onError = null
    }

    private fun mapErrorCode(errorCode: Int): String {
        return when (errorCode) {
            ScanCallback.SCAN_FAILED_ALREADY_STARTED ->
                "Scan already started"
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
                "Bluetooth scan application registration failed"
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED ->
                "BLE scan is not supported on this device"
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR ->
                "Bluetooth internal error"
            else -> "Unknown Bluetooth scan error (code $errorCode)"
        }
    }
    
    private fun createScanRecord(device: BluetoothDevice, rssi: Int): BluetoothScanRecord {
        return BluetoothScanRecord(
            name = device.name ?: "Unknown",
            address = device.address ?: "",
            rssi = rssi,
            deviceType = getDeviceType(device),
            timestamp = System.currentTimeMillis(),
            latitude = 0.0, // 将由LocationTracker提供
            longitude = 0.0 // 将由LocationTracker提供
        )
    }
    
    private fun getDeviceType(device: BluetoothDevice): String {
        return when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
            BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
            BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "Unknown"
            else -> "Unknown"
        }
    }
}