package com.example.scanapp.database

import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.models.CellScanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CellScanDao(private val database: ScanAppDatabase) {

    suspend fun insertOrUpdate(record: CellScanRecord) = withContext(DatabaseFactory.dbDispatcher) {
        database.transaction {
            insertOrUpdateInTransaction(record)
        }
    }

    suspend fun insertBatch(records: List<CellScanRecord>) = withContext(DatabaseFactory.dbDispatcher) {
        database.transaction {
            records.forEach { record ->
                insertOrUpdateInTransaction(record)
            }
        }
    }

    suspend fun getAllRecords(): List<CellScanRecord> = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.selectAllCellRecords().executeAsList().map { it.toModel() }
    }

    suspend fun getRecordByCellKey(cellKey: String): CellScanRecord? = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.selectCellByCellKey(cellKey).executeAsOneOrNull()?.toModel()
    }

    suspend fun getRecordsPaginated(limit: Int, offset: Int): List<CellScanRecord> = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.selectCellRecordsPaginated(limit = limit.toLong(), offset = offset.toLong()).executeAsList().map { it.toModel() }
    }

    suspend fun getRecordsPaginatedOrderedBySignal(limit: Int, offset: Int): List<CellScanRecord> = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.selectCellRecordsPaginatedBySignal(limit = limit.toLong(), offset = offset.toLong()).executeAsList().map { it.toModel() }
    }

    suspend fun getCount(): Long = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.countCellRecords().executeAsOne()
    }

    suspend fun getSeenTotal(): Long = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.sumCellCount().executeAsOne().IFNULL ?: 0L
    }

    suspend fun deleteAll() = withContext(DatabaseFactory.dbDispatcher) {
        database.databaseQueries.deleteAllCellRecords()
    }

    private fun com.example.scanapp.CellScanRecord.toModel() = CellScanRecord(
        id = id,
        cellKey = cellKey,
        networkType = networkType,
        operator = operator_,
        mcc = mcc.toInt(),
        mnc = mnc.toInt(),
        lac = lac,
        cid = cid,
        signalStrength = signalStrength.toInt(),
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        count = count.toInt()
    )

    private fun insertOrUpdateInTransaction(record: CellScanRecord) {
        val existing = database.databaseQueries.selectCellByCellKey(record.cellKey).executeAsOneOrNull()
        if (existing == null) {
            database.databaseQueries.insertCellRecord(
                cellKey = record.cellKey,
                networkType = record.networkType,
                operator_ = record.operator,
                mcc = record.mcc.toLong(),
                mnc = record.mnc.toLong(),
                lac = record.lac,
                cid = record.cid,
                signalStrength = record.signalStrength.toLong(),
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
            database.databaseQueries.updateCellRecord(
                networkType = record.networkType,
                operator_ = record.operator,
                signalStrength = record.signalStrength.toLong(),
                timestamp = record.timestamp,
                latitude = if (invalid) existing.latitude else record.latitude,
                longitude = if (invalid) existing.longitude else record.longitude,
                cellKey = record.cellKey
            )
        }
    }
}
