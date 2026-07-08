package com.example.scanapp

import android.app.Application
import android.util.Log
import com.example.scanapp.database.AndroidDatabaseDriver
import com.tencent.kuikly.core.android.KuiklyCoreEntry
import com.tencent.kuikly.core.render.android.adapter.IKRLogAdapter
import com.tencent.kuikly.core.render.android.adapter.IKRThreadAdapter
import com.tencent.kuikly.core.render.android.adapter.IKRUncaughtExceptionHandlerAdapter
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import java.util.concurrent.Executors

class ScanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupKuiklyAdapters()
        AndroidDatabaseDriver.initialize(this)
    }

    private fun setupKuiklyAdapters() {
        KuiklyRenderAdapterManager.krRouterAdapter = KRRouterAdapter
        KuiklyCoreEntry().triggerRegisterPages()

        if (KuiklyRenderAdapterManager.krLogAdapter == null) {
            KuiklyRenderAdapterManager.krLogAdapter = object : IKRLogAdapter {
                override val asyncLogEnable: Boolean = false

                override fun i(tag: String, msg: String) {
                    Log.i(tag, msg)
                }

                override fun d(tag: String, msg: String) {
                    Log.d(tag, msg)
                }

                override fun e(tag: String, msg: String) {
                    Log.e(tag, msg)
                }
            }
        }

        if (KuiklyRenderAdapterManager.krUncaughtExceptionHandlerAdapter == null) {
            KuiklyRenderAdapterManager.krUncaughtExceptionHandlerAdapter =
                object : IKRUncaughtExceptionHandlerAdapter {
                    override fun uncaughtException(throwable: Throwable) {
                        Log.e("Kuikly", "Uncaught exception on Kuikly thread", throwable)
                    }
                }
        }

        if (KuiklyRenderAdapterManager.krThreadAdapter == null) {
            KuiklyRenderAdapterManager.krThreadAdapter = object : IKRThreadAdapter {
                private val executor = Executors.newFixedThreadPool(2)

                override fun executeOnSubThread(task: () -> Unit) {
                    executor.execute(task)
                }
            }
        }
    }
}
