package com.example.scanapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

object PermissionHelper {

    @JvmStatic
    fun getAllForegroundPermissions(): Array<String> {
        val list = mutableListOf<String>()
        list.addAll(getLocationPermissions())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        return list.distinct().toTypedArray()
    }

    @JvmStatic
    fun getLocationPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    @JvmStatic
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

    @JvmStatic
    fun checkAndRequestPermissions(
        activity: ComponentActivity,
        onComplete: (Boolean) -> Unit
    ) {
        requestForegroundPermissions(activity) { foregroundGranted ->
            if (foregroundGranted) {
                requestBackgroundLocation(activity, onComplete)
            } else {
                onComplete(false)
            }
        }
    }

    private fun requestForegroundPermissions(
        activity: ComponentActivity,
        onResult: (Boolean) -> Unit
    ) {
        val needed = getAllForegroundPermissions().filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (needed.isEmpty()) {
            onResult(true)
            return
        }

        val launcher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grantResults ->
            onResult(grantResults.all { it.value })
        }
        launcher.launch(needed)
    }

    private fun requestBackgroundLocation(
        activity: ComponentActivity,
        onResult: (Boolean) -> Unit
    ) {
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

        val launcher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            onResult(granted)
        }
        launcher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }
}
