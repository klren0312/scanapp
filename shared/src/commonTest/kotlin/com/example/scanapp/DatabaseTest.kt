package com.example.scanapp

import com.example.scanapp.database.BluetoothScanDao
import com.example.scanapp.database.LocationDao
import com.example.scanapp.database.WifiScanDao
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.LocationRecord
import com.example.scanapp.models.WifiScanRecord
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseTest {

    @Test
    fun testWifiScanRecordCreation() {
        val record = WifiScanRecord(
            id = 1,
            ssid = "TestWiFi",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2400,
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074,
            count = 1
        )

        assertEquals(1L, record.id)
        assertEquals("TestWiFi", record.ssid)
        assertEquals("00:11:22:33:44:55", record.bssid)
        assertEquals(-50, record.signalStrength)
        assertEquals(2400, record.frequency)
        assertNotNull(record.timestamp)
        assertEquals(39.9042, record.latitude)
        assertEquals(116.4074, record.longitude)
        assertEquals(1, record.count)
    }

    @Test
    fun testWifiScanRecordWithDefaultValues() {
        val record = WifiScanRecord(
            ssid = "TestWiFi",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2400,
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074
        )

        assertEquals(0L, record.id)
        assertEquals(1, record.count)
    }

    @Test
    fun testWifiScanRecordCopy() {
        val original = WifiScanRecord(
            id = 1,
            ssid = "TestWiFi",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2400,
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074,
            count = 1
        )

        val copied = original.copy(signalStrength = -60, count = 2)

        assertEquals(-60, copied.signalStrength)
        assertEquals(2, copied.count)
        assertEquals(original.ssid, copied.ssid)
        assertEquals(original.bssid, copied.bssid)
    }

    @Test
    fun testBluetoothScanRecordCreation() {
        val record = BluetoothScanRecord(
            id = 1,
            name = "TestDevice",
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -60,
            deviceType = "BLE",
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074,
            count = 1
        )

        assertEquals(1L, record.id)
        assertEquals("TestDevice", record.name)
        assertEquals("AA:BB:CC:DD:EE:FF", record.address)
        assertEquals(-60, record.rssi)
        assertEquals("BLE", record.deviceType)
        assertNotNull(record.timestamp)
        assertEquals(39.9042, record.latitude)
        assertEquals(116.4074, record.longitude)
        assertEquals(1, record.count)
    }

    @Test
    fun testBluetoothScanRecordWithDefaultValues() {
        val record = BluetoothScanRecord(
            name = "TestDevice",
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -60,
            deviceType = "BLE",
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074
        )

        assertEquals(0L, record.id)
        assertEquals(1, record.count)
    }

    @Test
    fun testBluetoothScanRecordCopy() {
        val original = BluetoothScanRecord(
            id = 1,
            name = "TestDevice",
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -60,
            deviceType = "BLE",
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074,
            count = 1
        )

        val copied = original.copy(rssi = -70, count = 3)

        assertEquals(-70, copied.rssi)
        assertEquals(3, copied.count)
        assertEquals(original.name, copied.name)
        assertEquals(original.address, copied.address)
    }

    @Test
    fun testLocationRecordCreation() {
        val record = LocationRecord(
            id = 1,
            latitude = 39.9042,
            longitude = 116.4074,
            altitude = 50.0,
            accuracy = 10.0f,
            timestamp = System.currentTimeMillis()
        )

        assertEquals(1L, record.id)
        assertEquals(39.9042, record.latitude)
        assertEquals(116.4074, record.longitude)
        assertEquals(50.0, record.altitude)
        assertEquals(10.0f, record.accuracy)
        assertNotNull(record.timestamp)
    }

    @Test
    fun testLocationRecordWithDefaultValues() {
        val record = LocationRecord(
            latitude = 39.9042,
            longitude = 116.4074,
            altitude = 50.0,
            accuracy = 10.0f,
            timestamp = System.currentTimeMillis()
        )

        assertEquals(0L, record.id)
    }

    @Test
    fun testLocationRecordCopy() {
        val original = LocationRecord(
            id = 1,
            latitude = 39.9042,
            longitude = 116.4074,
            altitude = 50.0,
            accuracy = 10.0f,
            timestamp = System.currentTimeMillis()
        )

        val copied = original.copy(altitude = 100.0, accuracy = 5.0f)

        assertEquals(100.0, copied.altitude)
        assertEquals(5.0f, copied.accuracy)
        assertEquals(original.latitude, copied.latitude)
        assertEquals(original.longitude, copied.longitude)
    }

    @Test
    fun testMultipleWifiRecords() {
        val records = listOf(
            WifiScanRecord(
                ssid = "WiFi1",
                bssid = "00:11:22:33:44:55",
                signalStrength = -50,
                frequency = 2400,
                timestamp = System.currentTimeMillis(),
                latitude = 39.9042,
                longitude = 116.4074
            ),
            WifiScanRecord(
                ssid = "WiFi2",
                bssid = "AA:BB:CC:DD:EE:FF",
                signalStrength = -60,
                frequency = 5000,
                timestamp = System.currentTimeMillis(),
                latitude = 39.9043,
                longitude = 116.4075
            )
        )

        assertEquals(2, records.size)
        assertEquals("WiFi1", records[0].ssid)
        assertEquals("WiFi2", records[1].ssid)
        assertEquals(2400, records[0].frequency)
        assertEquals(5000, records[1].frequency)
    }

    @Test
    fun testMultipleBluetoothRecords() {
        val records = listOf(
            BluetoothScanRecord(
                name = "Device1",
                address = "AA:BB:CC:DD:EE:FF",
                rssi = -60,
                deviceType = "BLE",
                timestamp = System.currentTimeMillis(),
                latitude = 39.9042,
                longitude = 116.4074
            ),
            BluetoothScanRecord(
                name = "Device2",
                address = "11:22:33:44:55:66",
                rssi = -70,
                deviceType = "Classic",
                timestamp = System.currentTimeMillis(),
                latitude = 39.9043,
                longitude = 116.4075
            )
        )

        assertEquals(2, records.size)
        assertEquals("Device1", records[0].name)
        assertEquals("Device2", records[1].name)
        assertEquals("BLE", records[0].deviceType)
        assertEquals("Classic", records[1].deviceType)
    }
}
