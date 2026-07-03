package com.example.scanapp

import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.LocationRecord
import com.example.scanapp.models.WifiScanRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelsTest {

    @Test
    fun testWifiScanRecordCreation() {
        val record = WifiScanRecord(
            id = 1,
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2412,
            timestamp = System.currentTimeMillis(),
            latitude = 37.7749,
            longitude = -122.4194,
            count = 5
        )

        assertEquals(1L, record.id)
        assertEquals("TestNetwork", record.ssid)
        assertEquals("00:11:22:33:44:55", record.bssid)
        assertEquals(-50, record.signalStrength)
        assertEquals(2412, record.frequency)
        assertEquals(37.7749, record.latitude)
        assertEquals(-122.4194, record.longitude)
        assertEquals(5, record.count)
    }

    @Test
    fun testWifiScanRecordDefaults() {
        val record = WifiScanRecord(
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2412,
            timestamp = System.currentTimeMillis(),
            latitude = 37.7749,
            longitude = -122.4194
        )

        assertEquals(0L, record.id)
        assertEquals(1, record.count)
    }

    @Test
    fun testBluetoothScanRecordCreation() {
        val record = BluetoothScanRecord(
            id = 1,
            name = "MyBluetoothDevice",
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -60,
            deviceType = "Classic",
            timestamp = System.currentTimeMillis(),
            latitude = 37.7749,
            longitude = -122.4194,
            count = 3
        )

        assertEquals(1L, record.id)
        assertEquals("MyBluetoothDevice", record.name)
        assertEquals("AA:BB:CC:DD:EE:FF", record.address)
        assertEquals(-60, record.rssi)
        assertEquals("Classic", record.deviceType)
        assertEquals(37.7749, record.latitude)
        assertEquals(-122.4194, record.longitude)
        assertEquals(3, record.count)
    }

    @Test
    fun testBluetoothScanRecordDefaults() {
        val record = BluetoothScanRecord(
            name = "MyBluetoothDevice",
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -60,
            deviceType = "Classic",
            timestamp = System.currentTimeMillis(),
            latitude = 37.7749,
            longitude = -122.4194
        )

        assertEquals(0L, record.id)
        assertEquals(1, record.count)
    }

    @Test
    fun testLocationRecordCreation() {
        val record = LocationRecord(
            id = 1,
            latitude = 37.7749,
            longitude = -122.4194,
            altitude = 10.0,
            accuracy = 5.0f,
            timestamp = System.currentTimeMillis()
        )

        assertEquals(1L, record.id)
        assertEquals(37.7749, record.latitude)
        assertEquals(-122.4194, record.longitude)
        assertEquals(10.0, record.altitude)
        assertEquals(5.0f, record.accuracy)
    }

    @Test
    fun testLocationRecordDefaults() {
        val record = LocationRecord(
            latitude = 37.7749,
            longitude = -122.4194,
            altitude = 10.0,
            accuracy = 5.0f,
            timestamp = System.currentTimeMillis()
        )

        assertEquals(0L, record.id)
    }

    @Test
    fun testDataClassEquality() {
        val timestamp = System.currentTimeMillis()
        
        val record1 = WifiScanRecord(
            id = 1,
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2412,
            timestamp = timestamp,
            latitude = 37.7749,
            longitude = -122.4194,
            count = 1
        )
        
        val record2 = WifiScanRecord(
            id = 1,
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2412,
            timestamp = timestamp,
            latitude = 37.7749,
            longitude = -122.4194,
            count = 1
        )

        assertEquals(record1, record2)
        assertEquals(record1.hashCode(), record2.hashCode())
    }

    @Test
    fun testDataClassCopy() {
        val timestamp = System.currentTimeMillis()
        
        val original = WifiScanRecord(
            id = 1,
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2412,
            timestamp = timestamp,
            latitude = 37.7749,
            longitude = -122.4194,
            count = 1
        )
        
        val modified = original.copy(count = 10)

        assertEquals(1, original.count)
        assertEquals(10, modified.count)
        assertEquals(original.id, modified.id)
        assertEquals(original.ssid, modified.ssid)
    }
}
