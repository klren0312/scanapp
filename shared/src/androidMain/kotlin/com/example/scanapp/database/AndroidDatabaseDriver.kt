package com.example.scanapp.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.scanapp.db.ScanAppDatabase

object AndroidDatabaseDriver {
    private var context: Context? = null
    
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }
    
    fun createDriver(): SqlDriver {
        val appContext = context ?: throw IllegalStateException(
            "AndroidDatabaseDriver not initialized. Call AndroidDatabaseDriver.initialize(context) first."
        )
        return AndroidSqliteDriver(
            schema = ScanAppDatabase.Schema,
            context = appContext,
            name = "scanapp.db"
        )
    }
}

actual fun createSqlDriver(): SqlDriver {
    return AndroidDatabaseDriver.createDriver()
}