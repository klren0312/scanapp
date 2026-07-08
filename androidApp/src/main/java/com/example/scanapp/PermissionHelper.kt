package com.example.scanapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionHelper(private val activity: ComponentActivity) {

    private var foregroundCallback: ((Boolean) -> Unit)? = null

    private val foregroundLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        foregroundCallback?.invoke(grantResults.all { it.value })
    }

    fun checkAndRequestPermissions(onComplete: (Boolean) -> Unit) {
        requestForegroundPermissions(onComplete)
    }

    private fun requestForegroundPermissions(onResult: (Boolean) -> Unit) {
        val needed = getAllForegroundPermissions().filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (needed.isEmpty()) {
            onResult(true)
            return
        }

        foregroundCallback = onResult
        foregroundLauncher.launch(needed)
    }

    companion object {
        fun hasForegroundPermissions(activity: ComponentActivity): Boolean {
            return getAllForegroundPermissions().all {
                ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun getAllForegroundPermissions(): Array<String> {
            val list = mutableListOf<String>()
            list.addAll(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                list.add(Manifest.permission.BLUETOOTH_SCAN)
                list.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            return list.distinct().toTypedArray()
        }
    }
}
