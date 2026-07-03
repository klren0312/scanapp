package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.models.WifiScanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WifiScanDao(private val database: ScanAppDatabase) {

    suspend fun insertOrUpdate(record: WifiScanRecord) = withContext(Dispatchers.Default) {
        database.scanAppQueries.wifiUpsert(
            ssid = record.ssid,
            bssid = record.bssid,
            signalStrength = record.signalStrength,
            frequency = record.frequency,
            timestamp = record.timestamp,
            latitude = record.latitude,
            longitude = record.longitude
        )
    }

    suspend fun insertBatch(records: List<WifiScanRecord>) = withContext(Dispatchers.Default) {
        database.transaction {
            records.forEach { record ->
                database.scanAppQueries.wifiUpsert(
                    ssid = record.ssid,
                    bssid = record.bssid,
                    signalStrength = record.signalStrength,
                    frequency = record.frequency,
                    timestamp = record.timestamp,
                    latitude = record.latitude,
                    longitude = record.longitude
                )
            }
        }
    }

    suspend fun getAllRecords(): List<WifiScanRecord> = withContext(Dispatchers.Default) {
        database.scanAppQueries.selectAllWifiRecords().executeAsList().map {
            WifiScanRecord(
                id = it.id,
                ssid = it.ssid,
                bssid = it.bssid,
                signalStrength = it.signalStrength,
                frequency = it.frequency,
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count
            )
        }
    }

    suspend fun getRecordsPaginated(limit: Int, offset: Int): List<WifiScanRecord> = withContext(Dispatchers.Default) {
        database.scanAppQueries.selectWifiRecordsPaginated(limit = limit.toLong(), offset = offset.toLong()).executeAsList().map {
            WifiScanRecord(
                id = it.id,
                ssid = it.ssid,
                bssid = it.bssid,
                signalStrength = it.signalStrength,
                frequency = it.frequency,
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count
            )
        }
    }

    suspend fun getCount(): Long = withContext(Dispatchers.Default) {
        database.scanAppQueries.countWifiRecords().executeAsOne()
    }

    suspend fun getRecordsBySignalStrength(minSignalStrength: Int): List<WifiScanRecord> = withContext(Dispatchers.Default) {
        database.scanAppQueries.selectWifiRecordsBySignalStrength(minSignalStrength).executeAsList().map {
            WifiScanRecord(
                id = it.id,
                ssid = it.ssid,
                bssid = it.bssid,
                signalStrength = it.signalStrength,
                frequency = it.frequency,
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count
            )
        }
    }

    suspend fun getRecordsByFrequency(frequency: Int): List<WifiScanRecord> = withContext(Dispatchers.Default) {
        database.scanAppQueries.selectWifiRecordsByFrequency(frequency).executeAsList().map {
            WifiScanRecord(
                id = it.id,
                ssid = it.ssid,
                bssid = it.bssid,
                signalStrength = it.signalStrength,
                frequency = it.frequency,
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                count = it.count
            )
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.Default) {
        database.scanAppQueries.deleteAllWifiRecords()
    }
}
