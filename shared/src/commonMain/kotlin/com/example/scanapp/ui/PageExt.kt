package com.example.scanapp.ui

import com.example.scanapp.logging.CrashLogger
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.pager.Pager
import kotlin.coroutines.cancellation.CancellationException

fun Pager.safeLaunch(
    tag: String = "coroutine",
    block: suspend () -> Unit
) {
    lifecycleScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            CrashLogger.log(tag, e)
        }
    }
}

fun ViewContainer<*, *>.safe(tag: String = "render", block: ViewContainer<*, *>.() -> Unit) {
    try {
        this.block()
    } catch (e: Throwable) {
        CrashLogger.log(tag, e)
    }
}

fun safeValue(tag: String = "value", block: () -> String): String = try {
    block()
} catch (e: Throwable) {
    CrashLogger.log(tag, e)
    ""
}

fun safeBool(tag: String = "value", block: () -> Boolean): Boolean = try {
    block()
} catch (e: Throwable) {
    CrashLogger.log(tag, e)
    false
}
