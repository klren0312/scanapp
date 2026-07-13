package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.models.WifiScanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WifiScanDao(private val database: ScanAppDatabase) {

    suspend fun insertOrUpdate(record: WifiScanRecord) = withContext(Dispatchers.Default) {
        database.transaction {
            insertOrUpdateInTransaction(record)
        }
    }

    suspend fun insertBatch(records: List<WifiScanRecord>) = withContext(Dispatchers.Default) {
        database.transaction {
            records.forEach { record ->
                insertOrUpdateInTransaction(record)
            }
        }
    }

    suspend fun getAllRecords(): List<WifiScanRecord> = withContext(Dispatchers.Default) {
        database.databaseQueries.selectAllWifiRecords().executeAsList().map {
            WifiScanRecord(
                id = it.id,
                ssid = it.ssid,
                bssid = it.bssid,
                signalStrength = it.signalStrength.toInt(),
                frequency = it.frequency.toInt(),
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count.toInt()
            )
        }
    }

    suspend fun getRecordByBssid(bssid: String): WifiScanRecord? = withContext(Dispatchers.Default) {
        database.databaseQueries.selectWifiByBssid(bssid).executeAsOneOrNull()?.let {
            WifiScanRecord(
                id = it.id,
                ssid = it.ssid,
                bssid = it.bssid,
                signalStrength = it.signalStrength.toInt(),
                frequency = it.frequency.toInt(),
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count.toInt()
            )
        }
    }

    suspend fun getRecordsPaginated(limit: Int, offset: Int): List<WifiScanRecord> = withContext(Dispatchers.Default) {
        database.databaseQueries.selectWifiRecordsPaginated(limit = limit.toLong(), offset = offset.toLong()).executeAsList().map {
            WifiScanRecord(
                id = it.id,
                ssid = it.ssid,
                bssid = it.bssid,
                signalStrength = it.signalStrength.toInt(),
                frequency = it.frequency.toInt(),
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count.toInt()
            )
        }
    }

    suspend fun getCount(): Long = withContext(Dispatchers.Default) {
        database.databaseQueries.countWifiRecords().executeAsOne()
    }

    suspend fun getSeenTotal(): Long = withContext(Dispatchers.Default) {
        database.databaseQueries.sumWifiCount().executeAsOne().IFNULL ?: 0L
    }

    suspend fun getRecordsPaginatedOrderedBySignal(limit: Int, offset: Int): List<WifiScanRecord> = withContext(Dispatchers.Default) {
        database.databaseQueries.selectWifiRecordsPaginatedBySignal(limit = limit.toLong(), offset = offset.toLong()).executeAsList().map {
            WifiScanRecord(
                id = it.id,
                ssid = it.ssid,
                bssid = it.bssid,
                signalStrength = it.signalStrength.toInt(),
                frequency = it.frequency.toInt(),
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count.toInt()
            )
        }
    }

    suspend fun getRecordsBySignalStrength(minSignalStrength: Int): List<WifiScanRecord> = withContext(Dispatchers.Default) {
        database.databaseQueries.selectWifiRecordsBySignalStrength(minSignalStrength.toLong()).executeAsList().map {
            WifiScanRecord(
                id = it.id,
                ssid = it.ssid,
                bssid = it.bssid,
                signalStrength = it.signalStrength.toInt(),
                frequency = it.frequency.toInt(),
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count.toInt()
            )
        }
    }

    suspend fun getRecordsByFrequency(frequency: Int): List<WifiScanRecord> = withContext(Dispatchers.Default) {
        database.databaseQueries.selectWifiRecordsByFrequency(frequency.toLong()).executeAsList().map {
            WifiScanRecord(
                id = it.id,
                ssid = it.ssid,
                bssid = it.bssid,
                signalStrength = it.signalStrength.toInt(),
                frequency = it.frequency.toInt(),
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count.toInt()
            )
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.Default) {
        database.databaseQueries.deleteAllWifiRecords()
    }

    private fun insertOrUpdateInTransaction(record: WifiScanRecord) {
        val existing = database.databaseQueries.selectWifiByBssid(record.bssid).executeAsOneOrNull()
        if (existing == null) {
            database.databaseQueries.insertWifiRecord(
                ssid = record.ssid,
                bssid = record.bssid,
                signalStrength = record.signalStrength.toLong(),
                frequency = record.frequency.toLong(),
                timestamp = record.timestamp,
                latitude = record.latitude,
                longitude = record.longitude,
                count = record.count.toLong()
            )
        } else {
            database.databaseQueries.updateWifiRecord(
                ssid = record.ssid,
                signalStrength = record.signalStrength.toLong(),
                frequency = record.frequency.toLong(),
                timestamp = record.timestamp,
                latitude = record.latitude,
                longitude = record.longitude,
                bssid = record.bssid
            )
        }
    }
}
