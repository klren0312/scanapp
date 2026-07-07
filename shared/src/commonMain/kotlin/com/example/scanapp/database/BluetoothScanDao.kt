package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.models.BluetoothScanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BluetoothScanDao(private val database: ScanAppDatabase) {

    suspend fun insertOrUpdate(record: BluetoothScanRecord) = withContext(Dispatchers.Default) {
        database.transaction {
            insertOrUpdateInTransaction(record)
        }
    }

    suspend fun insertBatch(records: List<BluetoothScanRecord>) = withContext(Dispatchers.Default) {
        database.transaction {
            records.forEach { record ->
                insertOrUpdateInTransaction(record)
            }
        }
    }

    suspend fun getAllRecords(): List<BluetoothScanRecord> = withContext(Dispatchers.Default) {
        database.databaseQueries.selectAllBluetoothRecords().executeAsList().map {
            BluetoothScanRecord(
                id = it.id,
                name = it.name,
                address = it.address,
                rssi = it.rssi.toInt(),
                deviceType = it.deviceType,
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count.toInt()
            )
        }
    }

    suspend fun getRecordsPaginated(limit: Int, offset: Int): List<BluetoothScanRecord> = withContext(Dispatchers.Default) {
        database.databaseQueries.selectBluetoothRecordsPaginated(limit = limit.toLong(), offset = offset.toLong()).executeAsList().map {
            BluetoothScanRecord(
                id = it.id,
                name = it.name,
                address = it.address,
                rssi = it.rssi.toInt(),
                deviceType = it.deviceType,
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count.toInt()
            )
        }
    }

    suspend fun getCount(): Long = withContext(Dispatchers.Default) {
        database.databaseQueries.countBluetoothRecords().executeAsOne()
    }

    suspend fun getRecordsByDeviceType(deviceType: String): List<BluetoothScanRecord> = withContext(Dispatchers.Default) {
        database.databaseQueries.selectBluetoothRecordsByType(deviceType).executeAsList().map {
            BluetoothScanRecord(
                id = it.id,
                name = it.name,
                address = it.address,
                rssi = it.rssi.toInt(),
                deviceType = it.deviceType,
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count.toInt()
            )
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.Default) {
        database.databaseQueries.deleteAllBluetoothRecords()
    }

    private fun insertOrUpdateInTransaction(record: BluetoothScanRecord) {
        val existing = database.databaseQueries.selectBluetoothByAddress(record.address).executeAsOneOrNull()
        if (existing == null) {
            database.databaseQueries.insertBluetoothRecord(
                name = record.name,
                address = record.address,
                rssi = record.rssi.toLong(),
                deviceType = record.deviceType,
                timestamp = record.timestamp,
                latitude = record.latitude,
                longitude = record.longitude,
                count = record.count.toLong()
            )
        } else {
            database.databaseQueries.updateBluetoothRecord(
                name = record.name,
                rssi = record.rssi.toLong(),
                deviceType = record.deviceType,
                timestamp = record.timestamp,
                latitude = record.latitude,
                longitude = record.longitude,
                address = record.address
            )
        }
    }
}
