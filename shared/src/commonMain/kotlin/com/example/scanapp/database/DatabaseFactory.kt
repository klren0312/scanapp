package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase

object DatabaseFactory {
    private var instance: ScanAppDatabase? = null

    fun getDatabase(): ScanAppDatabase {
        return instance ?: synchronized(this) {
            instance ?: createDatabase().also { instance = it }
        }
    }

    private fun createDatabase(): ScanAppDatabase {
        val driver = createSqlDriver()
        return ScanAppDatabase(driver)
    }
}
