package com.example.scanapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var permissionHelper: PermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHelper = PermissionHelper(this)
        openScannerWhenReady()
    }

    private fun openScannerWhenReady() {
        if (PermissionHelper.hasForegroundPermissions(this)) {
            openScanner()
            return
        }

        permissionHelper.checkAndRequestPermissions { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "Scanner permissions are required for WiFi and Bluetooth scanning",
                    Toast.LENGTH_LONG
                ).show()
            }
            openScanner()
        }
    }

    private fun openScanner() {
        KuiklyRenderActivity.start(this, "Scanner")
        finish()
    }
}
