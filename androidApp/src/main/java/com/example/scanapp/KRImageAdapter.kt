package com.example.scanapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.LruCache
import com.tencent.kuikly.core.render.android.adapter.HRImageLoadOption
import com.tencent.kuikly.core.render.android.adapter.IKRImageAdapter
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

internal class KRImageAdapter(context: Context) : IKRImageAdapter {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newFixedThreadPool(3)
    private val bitmapCache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val pendingCallbacks = mutableMapOf<String, MutableList<(Drawable?) -> Unit>>()

    override val shouldWaitViewDidLoad: Boolean = false

    override fun fetchDrawable(
        imageLoadOption: HRImageLoadOption,
        callback: (Drawable?) -> Unit
    ) {
        val src = imageLoadOption.src
        bitmapCache.get(src)?.let { bitmap ->
            mainHandler.post {
                callback(BitmapDrawable(appContext.resources, bitmap))
            }
            return
        }

        val shouldLoad = synchronized(pendingCallbacks) {
            val callbacks = pendingCallbacks[src]
            if (callbacks != null) {
                callbacks.add(callback)
                false
            } else {
                pendingCallbacks[src] = mutableListOf(callback)
                true
            }
        }
        if (!shouldLoad) return

        runCatching {
            executor.execute {
                val bitmap = runCatching { loadBitmap(imageLoadOption) }
                    .onFailure { Log.w(TAG, "Image load failed: $src", it) }
                    .getOrNull()
                if (bitmap != null) bitmapCache.put(src, bitmap)
                completeLoad(src, bitmap)
            }
        }.onFailure { error ->
            Log.w(TAG, "Unable to schedule image load: $src", error)
            completeLoad(src, null)
        }
    }

    private fun completeLoad(src: String, bitmap: Bitmap?) {
        val callbacks = synchronized(pendingCallbacks) {
            pendingCallbacks.remove(src).orEmpty()
        }
        mainHandler.post {
            callbacks.forEach { callback ->
                callback(bitmap?.let { BitmapDrawable(appContext.resources, it) })
            }
        }
    }

    private fun loadBitmap(option: HRImageLoadOption): Bitmap? {
        return when {
            option.isWebUrl() -> loadWebBitmap(option.src)
            option.isAssets() -> appContext.assets
                .open(option.src.removePrefix("assets://"))
                .use(BitmapFactory::decodeStream)
            option.isFile() -> FileInputStream(option.src.removePrefix("file://"))
                .use(BitmapFactory::decodeStream)
            else -> null
        }
    }

    private fun loadWebBitmap(src: String): Bitmap? {
        val connection = URL(src).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "ScanApp/1.0 (Android)")
            connection.connect()
            if (connection.responseCode !in 200..299) {
                Log.w(TAG, "Image request returned HTTP ${connection.responseCode}: $src")
                null
            } else {
                connection.inputStream.use(BitmapFactory::decodeStream)
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TAG = "KRImageAdapter"
        private const val CACHE_BYTES = 8 * 1024 * 1024
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000
    }
}
