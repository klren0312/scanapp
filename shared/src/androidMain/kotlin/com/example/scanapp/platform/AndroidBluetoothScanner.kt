package com.example.scanapp.platform

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.scanapp.logging.CrashLogger
import com.example.scanapp.models.BluetoothScanRecord

class AndroidBluetoothScanner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val mainHandler = Handler(Looper.getMainLooper())
    private val stateLock = Any()

    private var bleScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var onDeviceFound: ((BluetoothScanRecord) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var retryRunnable: Runnable? = null
    private var retryCount = 0
    private var sessionId = 0L

    fun isBluetoothEnabled(): Boolean {
        return runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false)
    }

    fun startScan(
        callback: (BluetoothScanRecord) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        claimScanOwnership()?.stopLocalScan()

        val oldScanner: BluetoothLeScanner?
        val oldCallback: ScanCallback?
        val currentSession: Long
        synchronized(stateLock) {
            oldScanner = bleScanner
            oldCallback = scanCallback
            retryRunnable?.let(mainHandler::removeCallbacks)
            retryRunnable = null
            scanCallback = null
            bleScanner = null
            onDeviceFound = callback
            this.onError = onError
            retryCount = 0
            sessionId++
            currentSession = sessionId
        }
        stopCallback(oldScanner, oldCallback)

        if (!hasBlePermissions()) {
            failStart(
                currentSession,
                "Missing Bluetooth permission. Grant Nearby devices in app settings, then restart scanning."
            )
            return
        }

        if (!isBluetoothEnabled()) {
            failStart(currentSession, "Bluetooth is disabled. Please enable Bluetooth to scan.")
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            failStart(currentSession, "BluetoothLeScanner is unavailable on this device.")
            return
        }

        synchronized(stateLock) {
            if (currentSession != sessionId) return
            bleScanner = scanner
        }
        startScanAttempt(currentSession)
    }

    fun stopScan() {
        stopLocalScan()
        releaseScanOwnership()
    }

    private fun startScanAttempt(currentSession: Long) {
        val callback = createScanCallback(currentSession)
        val scanner = synchronized(stateLock) {
            if (currentSession != sessionId) return
            scanCallback = callback
            bleScanner
        } ?: return

        try {
            scanner.startScan(callback)
        } catch (e: SecurityException) {
            handleSynchronousFailure(
                currentSession,
                callback,
                "startScan threw SecurityException (Bluetooth permission missing?): ${e.message}"
            )
        } catch (e: Exception) {
            handleSynchronousFailure(currentSession, callback, "startScan failed: ${e.message}")
        }
    }

    private fun createScanCallback(currentSession: Long): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                dispatchResult(currentSession, this, result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { dispatchResult(currentSession, this, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                handleScanFailure(currentSession, this, errorCode)
            }
        }
    }

    private fun dispatchResult(currentSession: Long, callback: ScanCallback, result: ScanResult) {
        val listener = synchronized(stateLock) {
            if (currentSession != sessionId || scanCallback !== callback) return
            onDeviceFound
        }
        runCatching { createScanRecord(result.device, result.rssi) }
            .onSuccess { listener?.invoke(it) }
            .onFailure { error ->
                val msg = "Unable to read Bluetooth scan result: ${error.message}"
                android.util.Log.e(TAG, msg)
                CrashLogger.log(TAG, msg)
            }
    }

    private fun handleScanFailure(currentSession: Long, callback: ScanCallback, errorCode: Int) {
        val msg = mapErrorCode(errorCode)
        android.util.Log.e(TAG, "BLE scan failed: $msg")

        var retry: Runnable? = null
        var retryDelay = 0L
        var errorListener: ((String) -> Unit)? = null
        val scanner = synchronized(stateLock) {
            if (currentSession != sessionId || scanCallback !== callback) return
            scanCallback = null

            if (errorCode == ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED &&
                retryCount < RETRY_DELAYS_MS.size
            ) {
                retryDelay = RETRY_DELAYS_MS[retryCount]
                retryCount++
                retry = Runnable {
                    synchronized(stateLock) {
                        if (currentSession != sessionId || retryRunnable !== retry) return@Runnable
                        retryRunnable = null
                    }
                    startScanAttempt(currentSession)
                }
                retryRunnable = retry
            } else {
                errorListener = onError
            }
            bleScanner
        }

        stopCallback(scanner, callback)
        CrashLogger.log(TAG, "BLE scan failed: $msg (retryCount=$retryCount)")

        if (retry != null) {
            val retryMsg = "Retrying BLE scan in ${retryDelay}ms (attempt $retryCount/${RETRY_DELAYS_MS.size})"
            android.util.Log.w(TAG, retryMsg)
            CrashLogger.log(TAG, retryMsg)
            mainHandler.postDelayed(retry!!, retryDelay)
        } else {
            errorListener?.invoke(msg)
        }
    }

    private fun handleSynchronousFailure(
        currentSession: Long,
        callback: ScanCallback,
        message: String
    ) {
        val listener = synchronized(stateLock) {
            if (currentSession != sessionId || scanCallback !== callback) return
            scanCallback = null
            onError
        }
        android.util.Log.e(TAG, message)
        CrashLogger.log(TAG, message)
        listener?.invoke(message)
    }

    private fun failStart(currentSession: Long, message: String) {
        val listener = synchronized(stateLock) {
            if (currentSession != sessionId) return
            onError
        }
        android.util.Log.e(TAG, message)
        CrashLogger.log(TAG, message)
        stopLocalScan()
        releaseScanOwnership()
        listener?.invoke(message)
    }

    private fun stopLocalScan() {
        val scanner: BluetoothLeScanner?
        val callback: ScanCallback?
        synchronized(stateLock) {
            sessionId++
            retryRunnable?.let(mainHandler::removeCallbacks)
            retryRunnable = null
            scanner = bleScanner
            callback = scanCallback
            scanCallback = null
            bleScanner = null
            onDeviceFound = null
            onError = null
            retryCount = 0
        }
        stopCallback(scanner, callback)
    }

    private fun stopCallback(scanner: BluetoothLeScanner?, callback: ScanCallback?) {
        if (scanner == null || callback == null) return
        runCatching { scanner.stopScan(callback) }
            .onFailure { CrashLogger.log(TAG, "stopScan failed: ${it.message}") }
    }

    private fun hasBlePermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun mapErrorCode(errorCode: Int): String {
        return when (errorCode) {
            ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
                "Bluetooth scan application registration failed"
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scan is not supported on this device"
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Bluetooth internal error"
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
            latitude = 0.0,
            longitude = 0.0
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

    private fun claimScanOwnership(): AndroidBluetoothScanner? {
        return synchronized(ownerLock) {
            val previous = activeScanner
            activeScanner = this
            previous?.takeUnless { it === this }
        }
    }

    private fun releaseScanOwnership() {
        synchronized(ownerLock) {
            if (activeScanner === this) activeScanner = null
        }
    }

    companion object {
        private const val TAG = "AndroidBluetoothScanner"
        private val RETRY_DELAYS_MS = longArrayOf(5_000L, 15_000L, 30_000L)
        private val ownerLock = Any()
        private var activeScanner: AndroidBluetoothScanner? = null
    }
}
