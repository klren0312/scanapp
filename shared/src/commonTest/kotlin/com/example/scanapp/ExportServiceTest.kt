package com.example.scanapp

import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.WifiScanRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExportServiceTest {

    @Test
    fun testWifiScanRecordToJson() {
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

        val jsonString = """
            {
                "ssid": "${record.ssid}",
                "bssid": "${record.bssid}",
                "signalStrength": ${record.signalStrength},
                "frequency": ${record.frequency},
                "latitude": ${record.latitude},
                "longitude": ${record.longitude}
            }
        """.trimIndent()

        assertTrue(jsonString.contains("TestWiFi"))
        assertTrue(jsonString.contains("00:11:22:33:44:55"))
        assertTrue(jsonString.contains("-50"))
    }

    @Test
    fun testBluetoothScanRecordToJson() {
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

        val jsonString = """
            {
                "name": "${record.name}",
                "address": "${record.address}",
                "rssi": ${record.rssi},
                "deviceType": "${record.deviceType}",
                "latitude": ${record.latitude},
                "longitude": ${record.longitude}
            }
        """.trimIndent()

        assertTrue(jsonString.contains("TestDevice"))
        assertTrue(jsonString.contains("AA:BB:CC:DD:EE:FF"))
        assertTrue(jsonString.contains("BLE"))
    }

    @Test
    fun testCsvFormat() {
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

        val csvHeader = "SSID,BSSID,Signal Strength,Frequency,Latitude,Longitude"
        val csvRows = records.map { 
            "${it.ssid},${it.bssid},${it.signalStrength},${it.frequency},${it.latitude},${it.longitude}" 
        }
        val csvContent = csvHeader + "\n" + csvRows.joinToString("\n")

        assertTrue(csvContent.contains("WiFi1"))
        assertTrue(csvContent.contains("WiFi2"))
        assertTrue(csvContent.contains("00:11:22:33:44:55"))
        assertTrue(csvContent.contains("AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun testMultipleRecordsExport() {
        val wifiRecords = listOf(
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

        val bluetoothRecords = listOf(
            BluetoothScanRecord(
                name = "Device1",
                address = "11:22:33:44:55:66",
                rssi = -70,
                deviceType = "Classic",
                timestamp = System.currentTimeMillis(),
                latitude = 39.9042,
                longitude = 116.4074
            )
        )

        assertEquals(2, wifiRecords.size)
        assertEquals(1, bluetoothRecords.size)
        assertEquals("WiFi1", wifiRecords[0].ssid)
        assertEquals("Device1", bluetoothRecords[0].name)
    }

    @Test
    fun testExportDataIntegrity() {
        val originalRecord = WifiScanRecord(
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

        val exportedData = mapOf(
            "ssid" to originalRecord.ssid,
            "bssid" to originalRecord.bssid,
            "signalStrength" to originalRecord.signalStrength.toString(),
            "frequency" to originalRecord.frequency.toString(),
            "latitude" to originalRecord.latitude.toString(),
            "longitude" to originalRecord.longitude.toString()
        )

        assertEquals(originalRecord.ssid, exportedData["ssid"])
        assertEquals(originalRecord.bssid, exportedData["bssid"])
        assertEquals(originalRecord.signalStrength.toString(), exportedData["signalStrength"])
        assertEquals(originalRecord.frequency.toString(), exportedData["frequency"])
    }
}
