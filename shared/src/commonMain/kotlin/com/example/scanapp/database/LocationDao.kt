package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.models.LocationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationDao(private val database: ScanAppDatabase) {

    suspend fun insert(record: LocationRecord) = withContext(Dispatchers.Default) {
        database.scanAppQueries.insertLocationRecord(
            latitude = record.latitude,
            longitude = record.longitude,
            altitude = record.altitude,
            accuracy = record.accuracy,
            timestamp = record.timestamp
        )
    }

    suspend fun getAllRecords(): List<LocationRecord> = withContext(Dispatchers.Default) {
        database.scanAppQueries.selectAllLocationRecords().executeAsList().map {
            LocationRecord(
                id = it.id,
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude,
                accuracy = it.accuracy,
                timestamp = it.timestamp
            )
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.Default) {
        database.scanAppQueries.deleteAllLocationRecords()
    }
}
