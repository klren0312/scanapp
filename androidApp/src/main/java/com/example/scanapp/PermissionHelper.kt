package com.example.scanapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionHelper(private val activity: ComponentActivity) {

    private var foregroundCallback: ((Boolean) -> Unit)? = null
    private var backgroundCallback: ((Boolean) -> Unit)? = null

    private val foregroundLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        foregroundCallback?.invoke(grantResults.all { it.value })
    }

    private val backgroundLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        backgroundCallback?.invoke(granted)
    }

    fun checkAndRequestPermissions(onComplete: (Boolean) -> Unit) {
        requestForegroundPermissions { foregroundGranted ->
            if (foregroundGranted) {
                requestBackgroundLocation(onComplete)
            } else {
                onComplete(false)
            }
        }
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

    private fun requestBackgroundLocation(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            onResult(true)
            return
        }

        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onResult(true)
            return
        }

        backgroundCallback = onResult
        backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    companion object {
        fun hasAllPermissions(activity: ComponentActivity): Boolean {
            val foregroundGranted = getAllForegroundPermissions().all {
                ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
            }
            if (!foregroundGranted) return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
            return true
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
