package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase

object DatabaseFactory {
    private var instance: ScanAppDatabase? = null

    fun getDatabase(): ScanAppDatabase {
        instance?.let { return it }
        val db = createDatabase()
        instance = db
        return db
    }

    private fun createDatabase(): ScanAppDatabase {
        val driver = createSqlDriver()
        return ScanAppDatabase(driver)
    }
}
