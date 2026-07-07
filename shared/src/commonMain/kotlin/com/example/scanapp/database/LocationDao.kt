package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.models.LocationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationDao(private val database: ScanAppDatabase) {

    suspend fun insert(record: LocationRecord) = withContext(Dispatchers.Default) {
        database.databaseQueries.insertLocationRecord(
            latitude = record.latitude,
            longitude = record.longitude,
            altitude = record.altitude,
            accuracy = record.accuracy.toDouble(),
            timestamp = record.timestamp
        )
    }

    suspend fun insertBatch(records: List<LocationRecord>) = withContext(Dispatchers.Default) {
        database.transaction {
            records.forEach { record ->
                database.databaseQueries.insertLocationRecord(
                    latitude = record.latitude,
                    longitude = record.longitude,
                    altitude = record.altitude,
                    accuracy = record.accuracy.toDouble(),
                    timestamp = record.timestamp
                )
            }
        }
    }

    suspend fun getAllRecords(): List<LocationRecord> = withContext(Dispatchers.Default) {
        database.databaseQueries.selectAllLocationRecords().executeAsList().map {
            LocationRecord(
                id = it.id,
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude,
                accuracy = it.accuracy.toFloat(),
                timestamp = it.timestamp
            )
        }
    }

    suspend fun getRecordsPaginated(limit: Int, offset: Int): List<LocationRecord> = withContext(Dispatchers.Default) {
        database.databaseQueries.selectLocationRecordsPaginated(limit = limit.toLong(), offset = offset.toLong()).executeAsList().map {
            LocationRecord(
                id = it.id,
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude,
                accuracy = it.accuracy.toFloat(),
                timestamp = it.timestamp
            )
        }
    }

    suspend fun getCount(): Long = withContext(Dispatchers.Default) {
        database.databaseQueries.countLocationRecords().executeAsOne()
    }

    suspend fun deleteAll() = withContext(Dispatchers.Default) {
        database.databaseQueries.deleteAllLocationRecords()
    }
}
