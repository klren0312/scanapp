package com.example.scanapp.database

import app.cash.sqldelight.db.SqlDriver
import com.example.scanapp.db.ScanAppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseFactory {
    @Volatile
    private var instance: ScanAppDatabase? = null

    // All database reads/writes are confined to this single-threaded dispatcher so the
    // shared SQLDelight connection is never used from multiple threads at once. Concurrent
    // access from the background scanner and the UI pages previously caused
    // "active transaction" / database-locked crashes while scanning.
    val dbDispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)

    suspend fun getDatabase(): ScanAppDatabase = withContext(dbDispatcher) {
        instance ?: createDatabase().also { instance = it }
    }

    private fun createDatabase(): ScanAppDatabase {
        val driver = createSqlDriver()
        return ScanAppDatabase(driver)
    }
}
