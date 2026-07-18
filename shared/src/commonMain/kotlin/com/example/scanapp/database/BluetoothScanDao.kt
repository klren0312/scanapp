package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.models.BluetoothScanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BluetoothScanDao(private val database: ScanAppDatabase) {

    suspend fun insertOrUpdate(record: BluetoothScanRecord) = withContext(DatabaseFactory.dbDispatcher) {
        database.transaction {
            insertOrUpdateInTransaction(record)
        }
    }

    suspend fun insertBatch(records: List<BluetoothScanRecord>) = withContext(DatabaseFactory.dbDispatcher) {
        database.transaction {
            records.forEach { record ->
                insertOrUpdateInTransaction(record)
            }
        }
    }

    suspend fun getAllRecords(): List<BluetoothScanRecord> = withContext(DatabaseFactory.dbDispatcher) {
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

    suspend fun getRecordByAddress(address: String): BluetoothScanRecord? = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.selectBluetoothByAddress(address).executeAsOneOrNull()?.let {
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

    suspend fun getRecordsPaginated(limit: Int, offset: Int): List<BluetoothScanRecord> = withContext(DatabaseFactory.dbDispatcher) {
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

    suspend fun getCount(): Long = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.countBluetoothRecords().executeAsOne()
    }

    suspend fun getSeenTotal(): Long = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.sumBluetoothCount().executeAsOne().IFNULL ?: 0L
    }

    suspend fun getRecordsPaginatedOrderedByRssi(limit: Int, offset: Int): List<BluetoothScanRecord> = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.selectBluetoothRecordsPaginatedByRssi(limit = limit.toLong(), offset = offset.toLong()).executeAsList().map {
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

    suspend fun getRecordsByDeviceType(deviceType: String): List<BluetoothScanRecord> = withContext(DatabaseFactory.dbDispatcher) {
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

    suspend fun deleteAll() = withContext(DatabaseFactory.dbDispatcher) {
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
            // A scan without a GPS fix reports 0.0/NaN coordinates. Never overwrite a
            // previously stored valid location with an invalid one.
            val invalid = record.latitude.isNaN() || record.latitude.isInfinite() ||
                record.longitude.isNaN() || record.longitude.isInfinite() ||
                (record.latitude == 0.0 && record.longitude == 0.0)
            database.databaseQueries.updateBluetoothRecord(
                name = record.name,
                rssi = record.rssi.toLong(),
                deviceType = record.deviceType,
                timestamp = record.timestamp,
                latitude = if (invalid) existing.latitude else record.latitude,
                longitude = if (invalid) existing.longitude else record.longitude,
                address = record.address
            )
        }
    }
}
