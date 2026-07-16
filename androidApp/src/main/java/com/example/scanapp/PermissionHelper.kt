package com.example.scanapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionHelper(private val activity: ComponentActivity) {

    private var onCompleteCallback: ((Boolean) -> Unit)? = null

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        onCompleteCallback?.invoke(allGranted)
        onCompleteCallback = null
    }

    fun checkAndRequestPermissions(onComplete: (Boolean) -> Unit) {
        requestForegroundPermissions(onComplete)
    }

    private fun requestForegroundPermissions(onResult: (Boolean) -> Unit) {
        val needed = getAllForegroundPermissions().filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) {
            onResult(true)
            return
        }
        onCompleteCallback = onResult
        permissionLauncher.launch(needed.toTypedArray())
    }

    companion object {
        fun hasForegroundPermissions(activity: ComponentActivity): Boolean {
            return getAllForegroundPermissions().all {
                ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun getAllForegroundPermissions(): Array<String> {
            val list = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                list.add(Manifest.permission.BLUETOOTH_SCAN)
                list.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                list.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            return list.toTypedArray()
        }
    }
}