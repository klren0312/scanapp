package com.example.scanapp

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var permissionHelper: PermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHelper = PermissionHelper(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(50, 50, 50, 50)
        }

        val titleText = TextView(this).apply {
            text = "WiFi/蓝牙扫描器"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val scanButton = Button(this).apply {
            text = "开始扫描"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 50
            }
            setOnClickListener {
                navigateWithPermissionCheck("Scanner")
            }
        }

        val deviceListButton = Button(this).apply {
            text = "设备列表"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
            setOnClickListener {
                navigateWithPermissionCheck("DeviceList")
            }
        }

        val statisticsButton = Button(this).apply {
            text = "统计信息"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
            setOnClickListener {
                navigateWithPermissionCheck("Statistics")
            }
        }

        val mapButton = Button(this).apply {
            text = "地图视图"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
            setOnClickListener {
                navigateWithPermissionCheck {
                    startActivity(Intent(this@MainActivity, OsmMapActivity::class.java))
                }
            }
        }

        val settingsButton = Button(this).apply {
            text = "设置"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
            setOnClickListener {
                KuiklyRenderActivity.start(this@MainActivity, "Settings")
            }
        }

        layout.addView(titleText)
        layout.addView(scanButton)
        layout.addView(deviceListButton)
        layout.addView(statisticsButton)
        layout.addView(mapButton)
        layout.addView(settingsButton)

        setContentView(layout)
    }

    private fun navigateWithPermissionCheck(pageName: String) {
        navigateWithPermissionCheck {
            KuiklyRenderActivity.start(this, pageName)
        }
    }

    private fun navigateWithPermissionCheck(onGranted: () -> Unit) {
        if (PermissionHelper.hasForegroundPermissions(this)) {
            onGranted()
            return
        }

        permissionHelper.checkAndRequestPermissions { granted ->
            if (granted) {
                onGranted()
            } else {
                Toast.makeText(
                    this,
                    "需要授予权限才能使用此功能",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
