package com.example.scanapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import com.tencent.kuikly.core.render.android.KuiklyRenderView
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase

class KuiklyRenderActivity : AppCompatActivity() {

    private lateinit var kuiklyRenderView: KuiklyRenderView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pageName = intent.getStringExtra("pageName") ?: "Scanner"
        kuiklyRenderView = KuiklyRenderView(this, KuiklyRenderCoreExecuteModeBase.JVM, false)
        setContentView(kuiklyRenderView)

        val metrics = resources.displayMetrics
        kuiklyRenderView.init(
            pageName,
            pageName,
            emptyMap(),
            Size(metrics.widthPixels, metrics.heightPixels),
            ""
        )
    }

    override fun onResume() {
        super.onResume()
        if (::kuiklyRenderView.isInitialized) {
            kuiklyRenderView.resume()
        }
    }

    override fun onPause() {
        if (::kuiklyRenderView.isInitialized) {
            kuiklyRenderView.pause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (::kuiklyRenderView.isInitialized) {
            kuiklyRenderView.destroy()
        }
        super.onDestroy()
    }

    companion object {
        fun start(activity: Activity, pageName: String) {
            val intent = Intent(activity, KuiklyRenderActivity::class.java)
            intent.putExtra("pageName", pageName)
            activity.startActivity(intent)
        }
    }
}
