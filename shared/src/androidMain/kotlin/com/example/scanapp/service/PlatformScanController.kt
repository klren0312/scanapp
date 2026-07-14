package com.example.scanapp.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.scanapp.database.AndroidDatabaseDriver

actual object PlatformScanController {
    actual fun startBackgroundScanning(): ScanControlResult {
        return runCatching {
            BackgroundScanService.start(AndroidDatabaseDriver.requireContext())
            ScanControlResult(true, "Scanning started")
        }.getOrElse {
            ScanControlResult(false, "Start failed: ${it.message ?: it::class.simpleName}")
        }
    }

    actual fun stopBackgroundScanning(): ScanControlResult {
        return runCatching {
            BackgroundScanService.stop(AndroidDatabaseDriver.requireContext())
            ScanControlResult(true, "Scanning stopped")
        }.getOrElse {
            ScanControlResult(false, "Stop failed: ${it.message ?: it::class.simpleName}")
        }
    }

    actual fun isBluetoothEnabled(): Boolean {
        return runCatching {
            val context = AndroidDatabaseDriver.requireContext()
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            manager.adapter?.isEnabled == true
        }.getOrDefault(false)
    }

    actual fun requestEnableBluetooth(onEnabled: () -> Unit) {
        runCatching {
            val context = AndroidDatabaseDriver.requireContext()
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = manager.adapter ?: return@runCatching
            if (adapter.isEnabled) {
                onEnabled()
                return@runCatching
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON) {
                        runCatching { context.unregisterReceiver(this) }
                        onEnabled()
                    }
                }
            }
            context.registerReceiver(
                receiver,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
                Context.RECEIVER_NOT_EXPORTED
            )
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    actual fun openDeviceMap(latitude: Double, longitude: Double, title: String) {
        runCatching {
            val context = AndroidDatabaseDriver.requireContext()
            val intent = Intent().apply {
                setClassName(context, "com.example.scanapp.OsmMapActivity")
                putExtra("extra_lat", latitude)
                putExtra("extra_lon", longitude)
                putExtra("extra_title", title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
