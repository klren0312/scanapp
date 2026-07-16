package com.example.scanapp
import com.example.scanapp.ActivityHolder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tencent.kuikly.core.render.android.css.ktx.toMap
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.expand.KuiklyBaseView
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate
import org.json.JSONObject

class KuiklyRenderActivity : AppCompatActivity() {

    private lateinit var kuiklyView: KuiklyBaseView
    private var pageName: String = "Scanner"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pageName = intent.getStringExtra("pageName") ?: "Scanner"
        kuiklyView = KuiklyBaseView(this, createKuiklyDelegate())
        setContentView(kuiklyView)
        kuiklyView.onAttach("", pageName, readPageData())
    }

    override fun onResume() {
        super.onResume()
        ActivityHolder.set(this)
        if (::kuiklyView.isInitialized) {
            kuiklyView.onResume()
        }
    }

    override fun onPause() {
        ActivityHolder.clear(this)
        if (::kuiklyView.isInitialized) {
            kuiklyView.onPause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        ActivityHolder.clear(this)
        if (::kuiklyView.isInitialized) {
            kuiklyView.onDetach()
        }
        ActivityHolder.clear(this)
        super.onDestroy()
    }

    private fun createKuiklyDelegate(): KuiklyRenderViewBaseDelegatorDelegate {
        return object : KuiklyRenderViewBaseDelegatorDelegate {
            override fun onPageLoadComplete(
                isSucceed: Boolean,
                errorReason: ErrorReason?,
                executeMode: KuiklyRenderCoreExecuteModeBase
            ) {
                if (!isSucceed) {
                    val message = "Kuikly page load failed: $pageName, reason=$errorReason"
                    Log.e(TAG, message)
                    Toast.makeText(this@KuiklyRenderActivity, message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onUnhandledException(
                throwable: Throwable,
                errorReason: ErrorReason,
                executeMode: KuiklyRenderCoreExecuteModeBase
            ) {
                Log.e(TAG, "Kuikly page error: $pageName, reason=$errorReason", throwable)
            }
        }
    }

    companion object {
        private const val TAG = "KuiklyRenderActivity"

        init {
            KuiklyRenderAdapterManager.krRouterAdapter = KRRouterAdapter
        }

        fun start(activity: Activity, pageName: String) {
            start(activity as Context, pageName)
        }

        fun start(context: Context, pageName: String) {
            start(context, pageName, JSONObject())
        }

        fun start(context: Context, pageName: String, pageData: JSONObject) {
            val intent = Intent(context, KuiklyRenderActivity::class.java)
            intent.putExtra("pageName", pageName)
            intent.putExtra("pageData", pageData.toString())
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun readPageData(): Map<String, Any> {
        val pageDataJson = intent.getStringExtra("pageData") ?: return emptyMap()
        return runCatching {
            JSONObject(pageDataJson).toMap()
        }.getOrDefault(emptyMap())
    }
}
