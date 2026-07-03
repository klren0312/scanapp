package com.example.scanapp

import app.cash.sqldelight.driver.jdbc.JdbcSqliteDriver
import com.example.scanapp.db.ScanAppDatabase
import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.LocationDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.LocationRecord
import com.example.scanapp.models.WifiScanRecord
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseTest {
    private lateinit var database: ScanAppDatabase
    private lateinit var wifiDao: WifiScanDao
    private lateinit var bluetoothDao: BluetoothScanDao
    private lateinit var locationDao: LocationDao

    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver("jdbc:sqlite:memory:")
        ScanAppDatabase.Schema.create(driver)
        database = ScanAppDatabase(driver)
        wifiDao = WifiScanDao(database)
        bluetoothDao = BluetoothScanDao(database)
        locationDao = LocationDao(database)
    }

    @AfterTest
    fun tearDown() = runBlocking {
        wifiDao.deleteAll()
        bluetoothDao.deleteAll()
        locationDao.deleteAll()
    }

    @Test
    fun wifiInsertOrUpdate_newRecord_inserts() = runBlocking {
        wifiDao.insertOrUpdate(WifiScanRecord(
            ssid = "TestWiFi", bssid = "00:11:22:33:44:55",
            signalStrength = -50, frequency = 2400,
            timestamp = 1000L, latitude = 0.0, longitude = 0.0, count = 1
        ))
        assertEquals(1L, wifiDao.getCount())
    }

    @Test
    fun wifiInsertOrUpdate_duplicateBssid_incrementsCount() = runBlocking {
        wifiDao.insertOrUpdate(WifiScanRecord(
            ssid = "TestWiFi", bssid = "00:11:22:33:44:55",
            signalStrength = -50, frequency = 2400,
            timestamp = 1000L, latitude = 0.0, longitude = 0.0, count = 1
        ))
        wifiDao.insertOrUpdate(WifiScanRecord(
            ssid = "TestWiFi", bssid = "00:11:22:33:44:55",
            signalStrength = -60, frequency = 2400,
            timestamp = 2000L, latitude = 1.0, longitude = 1.0, count = 1
        ))
        assertEquals(1L, wifiDao.getCount())
        val records = wifiDao.getAllRecords()
        assertEquals(1, records.size)
        assertTrue(records[0].count >= 2, "Count should increment on duplicate BSSID")
    }

    @Test
    fun wifiInsertBatch_multipleRecords() = runBlocking {
        wifiDao.insertBatch(listOf(
            WifiScanRecord("WiFi1", "00:11:22:33:44:01", -50, 2400, 1000L, 0.0, 0.0, 1),
            WifiScanRecord("WiFi2", "00:11:22:33:44:02", -60, 5000, 1000L, 0.0, 0.0, 1)
        ))
        assertEquals(2L, wifiDao.getCount())
    }

    @Test
    fun wifiGetAllRecords_returnsAll() = runBlocking {
        wifiDao.insertBatch(listOf(
            WifiScanRecord("WiFi1", "00:11:22:33:44:01", -50, 2400, 1000L, 0.0, 0.0, 1),
            WifiScanRecord("WiFi2", "00:11:22:33:44:02", -60, 5000, 2000L, 0.0, 0.0, 1),
            WifiScanRecord("WiFi3", "00:11:22:33:44:03", -70, 2400, 3000L, 0.0, 0.0, 1)
        ))
        assertEquals(3, wifiDao.getAllRecords().size)
    }

    @Test
    fun wifiGetRecordsPaginated_returnsCorrectPage() = runBlocking {
        wifiDao.insertBatch((1..10).map {
            WifiScanRecord("WiFi$it", "00:11:22:33:44:${it.toString().padStart(2, '0')}", -50, 2400, it * 1000L, 0.0, 0.0, 1)
        })
        val page1 = wifiDao.getRecordsPaginated(5, 0)
        val page2 = wifiDao.getRecordsPaginated(5, 5)
        assertEquals(5, page1.size)
        assertEquals(5, page2.size)
        assertTrue(page1[0].timestamp >= page1[4].timestamp, "Results should be ordered by timestamp DESC")
    }

    @Test
    fun wifiDeleteAll_clearsRecords() = runBlocking {
        wifiDao.insertOrUpdate(WifiScanRecord("Test", "00:11:22:33:44:55", -50, 2400, 1000L, 0.0, 0.0, 1))
        assertEquals(1L, wifiDao.getCount())
        wifiDao.deleteAll()
        assertEquals(0L, wifiDao.getCount())
    }

    @Test
    fun wifiGetCount_zeroWhenEmpty() = runBlocking {
        assertEquals(0L, wifiDao.getCount())
    }

    @Test
    fun bluetoothInsertOrUpdate_newRecord_inserts() = runBlocking {
        bluetoothDao.insertOrUpdate(BluetoothScanRecord("BT1", "AA:BB:CC:DD:EE:FF", -70, "LE", 1000L, 0.0, 0.0, 1))
        assertEquals(1L, bluetoothDao.getCount())
    }

    @Test
    fun bluetoothInsertOrUpdate_duplicateAddress_incrementsCount() = runBlocking {
        bluetoothDao.insertOrUpdate(BluetoothScanRecord("BT1", "AA:BB:CC:DD:EE:FF", -70, "LE", 1000L, 0.0, 0.0, 1))
        bluetoothDao.insertOrUpdate(BluetoothScanRecord("BT1", "AA:BB:CC:DD:EE:FF", -80, "LE", 2000L, 1.0, 1.0, 1))
        assertEquals(1L, bluetoothDao.getCount())
        assertTrue(bluetoothDao.getAllRecords()[0].count >= 2, "Count should increment on duplicate address")
    }

    @Test
    fun bluetoothInsertBatch_multipleRecords() = runBlocking {
        bluetoothDao.insertBatch(listOf(
            BluetoothScanRecord("BT1", "AA:BB:CC:DD:EE:01", -70, "LE", 1000L, 0.0, 0.0, 1),
            BluetoothScanRecord("BT2", "AA:BB:CC:DD:EE:02", -80, "Classic", 2000L, 0.0, 0.0, 1)
        ))
        assertEquals(2L, bluetoothDao.getCount())
    }

    @Test
    fun bluetoothGetAllRecords_returnsAll() = runBlocking {
        bluetoothDao.insertBatch(listOf(
            BluetoothScanRecord("BT1", "AA:BB:CC:DD:EE:01", -70, "LE", 1000L, 0.0, 0.0, 1),
            BluetoothScanRecord("BT2", "AA:BB:CC:DD:EE:02", -80, "Classic", 2000L, 0.0, 0.0, 1)
        ))
        val records = bluetoothDao.getAllRecords()
        assertEquals(2, records.size)
        assertTrue(records[0].timestamp >= records[1].timestamp, "Results should be ordered by timestamp DESC")
    }

    @Test
    fun bluetoothDeleteAll_clearsRecords() = runBlocking {
        bluetoothDao.insertOrUpdate(BluetoothScanRecord("BT1", "AA:BB:CC:DD:EE:FF", -70, "LE", 1000L, 0.0, 0.0, 1))
        assertEquals(1L, bluetoothDao.getCount())
        bluetoothDao.deleteAll()
        assertEquals(0L, bluetoothDao.getCount())
    }

    @Test
    fun locationInsertAndQuery() = runBlocking {
        locationDao.insert(LocationRecord(39.9, 116.4, 50.0, 10.0f, 1000L))
        assertEquals(1L, locationDao.getCount())
        val all = locationDao.getAllRecords()
        assertEquals(1, all.size)
        assertEquals(39.9, all[0].latitude, 0.001)
        assertEquals(116.4, all[0].longitude, 0.001)
        assertEquals(50.0, all[0].altitude, 0.001)
        assertEquals(10.0f, all[0].accuracy, 0.001f)
    }

    @Test
    fun locationInsertBatch() = runBlocking {
        locationDao.insertBatch(listOf(
            LocationRecord(39.9, 116.4, 50.0, 10.0f, 1000L),
            LocationRecord(40.0, 117.0, 100.0, 5.0f, 2000L)
        ))
        assertEquals(2L, locationDao.getCount())
    }

    @Test
    fun locationGetRecordsPaginated() = runBlocking {
        locationDao.insertBatch((1..10).map {
            LocationRecord(39.9 + it * 0.1, 116.4, 50.0, 10.0f, it * 1000L)
        })
        val page1 = locationDao.getRecordsPaginated(4, 0)
        val page2 = locationDao.getRecordsPaginated(4, 4)
        assertEquals(4, page1.size)
        assertEquals(4, page2.size)
    }

    @Test
    fun locationGetAllRecords_orderedByTimestamp() = runBlocking {
        locationDao.insert(LocationRecord(39.0, 116.0, 10.0, 1.0f, 3000L))
        locationDao.insert(LocationRecord(40.0, 117.0, 20.0, 2.0f, 1000L))
        locationDao.insert(LocationRecord(41.0, 118.0, 30.0, 3.0f, 2000L))
        val all = locationDao.getAllRecords()
        assertEquals(3, all.size)
        assertTrue(all[0].timestamp >= all[1].timestamp)
        assertTrue(all[1].timestamp >= all[2].timestamp)
    }

    @Test
    fun locationDeleteAll() = runBlocking {
        locationDao.insert(LocationRecord(39.9, 116.4, 50.0, 10.0f, 1000L))
        assertEquals(1L, locationDao.getCount())
        locationDao.deleteAll()
        assertEquals(0L, locationDao.getCount())
    }
}
