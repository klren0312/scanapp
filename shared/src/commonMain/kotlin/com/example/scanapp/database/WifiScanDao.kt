package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.models.WifiScanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WifiScanDao(private val database: ScanAppDatabase) {

    suspend fun insertOrUpdate(record: WifiScanRecord) = withContext(Dispatchers.Default) {
        val existing = database.scanAppQueries.selectWifiByBssid(record.bssid).executeAsOneOrNull()

        if (existing != null) {
            database.scanAppQueries.updateWifiCount(record.bssid)
        } else {
            database.scanAppQueries.insertWifiRecord(
                ssid = record.ssid,
                bssid = record.bssid,
                signalStrength = record.signalStrength,
                frequency = record.frequency,
                timestamp = record.timestamp,
                latitude = record.latitude,
                longitude = record.longitude,
                count = record.count
            )
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

    suspend fun deleteAll() = withContext(Dispatchers.Default) {
        database.scanAppQueries.deleteAllWifiRecords()
    }
}
