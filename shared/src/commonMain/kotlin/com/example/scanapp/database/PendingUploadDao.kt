package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PendingUploadDao(private val database: ScanAppDatabase) {

    suspend fun enqueue(payload: String, createdAt: Long) = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.enqueuePendingUpload(payload, createdAt)
    }

    suspend fun peekOldest(limit: Long): List<PendingUploadRow> = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.selectOldestPendingUploads(limit).executeAsList().map {
            PendingUploadRow(it.id, it.payload)
        }
    }

    suspend fun deleteUpTo(maxId: Long) = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.deletePendingUploadsUpTo(maxId)
    }

    suspend fun count(): Long = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.countPendingUploads().executeAsOne()
    }
}

data class PendingUploadRow(val id: Long, val payload: String)
