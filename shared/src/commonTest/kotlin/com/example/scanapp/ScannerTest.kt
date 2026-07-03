package com.example.scanapp

import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScannerTest {

    @Test
    fun testWifiScanRecord() {
        val record = WifiScanRecord(
            ssid = "TestWiFi",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2400,
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074
        )

        assertEquals("TestWiFi", record.ssid)
        assertEquals("00:11:22:33:44:55", record.bssid)
        assertEquals(-50, record.signalStrength)
        assertEquals(2400, record.frequency)
        assertNotNull(record.timestamp)
        assertEquals(39.9042, record.latitude)
        assertEquals(116.4074, record.longitude)
    }

    @Test
    fun testBluetoothScanRecord() {
        val record = BluetoothScanRecord(
            name = "TestDevice",
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -60,
            deviceType = "BLE",
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074
        )

        assertEquals("TestDevice", record.name)
        assertEquals("AA:BB:CC:DD:EE:FF", record.address)
        assertEquals(-60, record.rssi)
        assertEquals("BLE", record.deviceType)
        assertNotNull(record.timestamp)
        assertEquals(39.9042, record.latitude)
        assertEquals(116.4074, record.longitude)
    }

    @Test
    fun testScannerStateFlow() = runTest {
        val isScanning = MutableStateFlow(false)
        val wifiDevices = MutableStateFlow<List<WifiScanRecord>>(emptyList())
        val bluetoothDevices = MutableStateFlow<List<BluetoothScanRecord>>(emptyList())

        assertFalse(isScanning.value)
        assertTrue(wifiDevices.value.isEmpty())
        assertTrue(bluetoothDevices.value.isEmpty())

        isScanning.value = true
        assertTrue(isScanning.value)

        val testWifiDevice = WifiScanRecord(
            ssid = "TestWiFi",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2400,
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074
        )
        wifiDevices.value = listOf(testWifiDevice)
        assertEquals(1, wifiDevices.value.size)
        assertEquals("TestWiFi", wifiDevices.value[0].ssid)

        val testBluetoothDevice = BluetoothScanRecord(
            name = "TestDevice",
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -60,
            deviceType = "BLE",
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074
        )
        bluetoothDevices.value = listOf(testBluetoothDevice)
        assertEquals(1, bluetoothDevices.value.size)
        assertEquals("TestDevice", bluetoothDevices.value[0].name)
    }

    @Test
    fun testSignalStrengthRange() {
        val strongSignal = WifiScanRecord(
            ssid = "StrongWiFi",
            bssid = "00:11:22:33:44:55",
            signalStrength = -30,
            frequency = 2400,
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074
        )

        val weakSignal = WifiScanRecord(
            ssid = "WeakWiFi",
            bssid = "AA:BB:CC:DD:EE:FF",
            signalStrength = -90,
            frequency = 2400,
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074
        )

        assertTrue(strongSignal.signalStrength > weakSignal.signalStrength)
        assertEquals(-30, strongSignal.signalStrength)
        assertEquals(-90, weakSignal.signalStrength)
    }

    @Test
    fun testDeviceTypes() {
        val deviceTypes = listOf("BLE", "Classic", "Dual Mode", "Unknown")

        deviceTypes.forEach { type ->
            val record = BluetoothScanRecord(
                name = "Device $type",
                address = "AA:BB:CC:DD:EE:FF",
                rssi = -60,
                deviceType = type,
                timestamp = System.currentTimeMillis(),
                latitude = 39.9042,
                longitude = 116.4074
            )
            assertEquals(type, record.deviceType)
        }
    }

    @Test
    fun testFrequencyBands() {
        val wifi24Ghz = WifiScanRecord(
            ssid = "WiFi24",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2400,
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074
        )

        val wifi5Ghz = WifiScanRecord(
            ssid = "WiFi5",
            bssid = "AA:BB:CC:DD:EE:FF",
            signalStrength = -50,
            frequency = 5000,
            timestamp = System.currentTimeMillis(),
            latitude = 39.9042,
            longitude = 116.4074
        )

        assertEquals(2400, wifi24Ghz.frequency)
        assertEquals(5000, wifi5Ghz.frequency)
    }

    @Test
    fun testRecordEquality() {
        val timestamp = System.currentTimeMillis()

        val record1 = WifiScanRecord(
            ssid = "TestWiFi",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2400,
            timestamp = timestamp,
            latitude = 39.9042,
            longitude = 116.4074
        )

        val record2 = WifiScanRecord(
            ssid = "TestWiFi",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2400,
            timestamp = timestamp,
            latitude = 39.9042,
            longitude = 116.4074
        )

        assertEquals(record1, record2)
        assertEquals(record1.hashCode(), record2.hashCode())
    }

    @Test
    fun testRecordInequality() {
        val timestamp = System.currentTimeMillis()

        val record1 = WifiScanRecord(
            ssid = "WiFi1",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2400,
            timestamp = timestamp,
            latitude = 39.9042,
            longitude = 116.4074
        )

        val record2 = WifiScanRecord(
            ssid = "WiFi2",
            bssid = "00:11:22:33:44:55",
            signalStrength = -50,
            frequency = 2400,
            timestamp = timestamp,
            latitude = 39.9042,
            longitude = 116.4074
        )

        assertFalse(record1 == record2)
    }
}
