package com.example.scanapp.logging

import kotlinx.coroutines.CoroutineExceptionHandler

data class LogEntry(
    val id: Long,
    val tag: String,
    val message: String,
    val stack: String
)

object CrashLogger {
    private val entries = mutableListOf<LogEntry>()
    private var nextId = 1L
    private const val MAX_ENTRIES = 200

    fun log(tag: String, message: String) {
        record(tag, message, "")
    }

    fun log(tag: String, throwable: Throwable) {
        record(tag, throwable.message ?: throwable.toString(), throwable.stackTraceToString())
    }

    fun all(): List<LogEntry> = entries.toList()

    fun clear() = entries.clear()

    fun exceptionHandler(tag: String = "coroutine") = CoroutineExceptionHandler { _, throwable ->
        log(tag, throwable)
    }

    private fun record(tag: String, message: String, stack: String) {
        val id = nextId++
        val logEntry = LogEntry(id, tag, message, stack)
        entries.add(0, logEntry)
        if (entries.size > MAX_ENTRIES) {
            entries.subList(MAX_ENTRIES, entries.size).clear()
        }
        println("[CrashLogger][$tag] #${logEntry.id} $message")
        if (stack.isNotEmpty()) println(stack)
    }
}
