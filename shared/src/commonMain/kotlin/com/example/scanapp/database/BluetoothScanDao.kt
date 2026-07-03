package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.models.BluetoothScanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BluetoothScanDao(private val database: ScanAppDatabase) {

    suspend fun insertOrUpdate(record: BluetoothScanRecord) = withContext(Dispatchers.Default) {
        val existing = database.scanAppQueries.selectBluetoothByAddress(record.address).executeAsOneOrNull()

        if (existing != null) {
            database.scanAppQueries.updateBluetoothCount(record.address)
        } else {
            database.scanAppQueries.insertBluetoothRecord(
                name = record.name,
                address = record.address,
                rssi = record.rssi,
                deviceType = record.deviceType,
                timestamp = record.timestamp,
                latitude = record.latitude,
                longitude = record.longitude,
                count = record.count
            )
        }
    }

    suspend fun getAllRecords(): List<BluetoothScanRecord> = withContext(Dispatchers.Default) {
        database.scanAppQueries.selectAllBluetoothRecords().executeAsList().map {
            BluetoothScanRecord(
                id = it.id,
                name = it.name,
                address = it.address,
                rssi = it.rssi,
                deviceType = it.deviceType,
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count
            )
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.Default) {
        database.scanAppQueries.deleteAllBluetoothRecords()
    }
}
