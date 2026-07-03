package com.example.scanapp

import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.LocationRecord
import com.example.scanapp.models.WifiScanRecord
import com.example.scanapp.service.ExportServiceImpl
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.int
import kotlinx.serialization.json.double
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScannerTest {
    private val exportService = ExportServiceImpl()

    @Test
    fun exportToCsv_emptyLists_returnsHeaders() = runBlocking {
        val csv = exportService.exportToCsv(emptyList(), emptyList(), emptyList())
        assertTrue(csv.contains("WiFi Records"))
        assertTrue(csv.contains("Bluetooth Records"))
        assertTrue(csv.contains("Location Records"))
        assertTrue(csv.contains("SSID,BSSID,Signal Strength,Frequency,Timestamp,Latitude,Longitude,Count"))
        assertTrue(csv.contains("Name,Address,RSSI,Device Type,Timestamp,Latitude,Longitude,Count"))
        assertTrue(csv.contains("Latitude,Longitude,Altitude,Accuracy,Timestamp"))
    }

    @Test
    fun exportToCsv_wifiRecords_includesData() = runBlocking {
        val csv = exportService.exportToCsv(
            listOf(WifiScanRecord("TestWiFi", "00:11:22:33:44:55", -50, 2400, 1000L, 39.9, 116.4, 3)),
            emptyList(), emptyList()
        )
        assertTrue(csv.contains("TestWiFi"))
        assertTrue(csv.contains("00:11:22:33:44:55"))
        assertTrue(csv.contains("-50"))
        assertTrue(csv.contains("2400"))
        assertTrue(csv.contains("3"))
    }

    @Test
    fun exportToCsv_bluetoothRecords_includesData() = runBlocking {
        val csv = exportService.exportToCsv(
            emptyList(),
            listOf(BluetoothScanRecord("TestBT", "AA:BB:CC:DD:EE:FF", -70, "LE", 1000L, 39.9, 116.4, 5)),
            emptyList()
        )
        assertTrue(csv.contains("TestBT"))
        assertTrue(csv.contains("AA:BB:CC:DD:EE:FF"))
        assertTrue(csv.contains("-70"))
        assertTrue(csv.contains("LE"))
        assertTrue(csv.contains("5"))
    }

    @Test
    fun exportToCsv_locationRecords_includesData() = runBlocking {
        val csv = exportService.exportToCsv(
            emptyList(), emptyList(),
            listOf(LocationRecord(39.9, 116.4, 50.0, 10.0f, 1000L))
        )
        assertTrue(csv.contains("39.9"))
        assertTrue(csv.contains("116.4"))
        assertTrue(csv.contains("50.0"))
        assertTrue(csv.contains("10.0"))
        assertTrue(csv.contains("1000"))
    }

    @Test
    fun exportToCsv_escapesCommas() = runBlocking {
        val csv = exportService.exportToCsv(
            listOf(WifiScanRecord("WiFi, Inc.", "00:11:22:33:44:55", -50, 2400, 1000L, 0.0, 0.0, 1)),
            emptyList(), emptyList()
        )
        assertTrue(csv.contains("\"WiFi, Inc.\""))
    }

    @Test
    fun exportToCsv_escapesQuotes() = runBlocking {
        val csv = exportService.exportToCsv(
            listOf(WifiScanRecord("WiFi \"Office\"", "00:11:22:33:44:55", -50, 2400, 1000L, 0.0, 0.0, 1)),
            emptyList(), emptyList()
        )
        assertTrue(csv.contains("\"WiFi \"\"Office\"\"\""))
    }

    @Test
    fun exportToCsv_escapesNewlines() = runBlocking {
        val csv = exportService.exportToCsv(
            emptyList(),
            listOf(BluetoothScanRecord("BT\nDevice", "AA:BB:CC:DD:EE:FF", -70, "LE", 1000L, 0.0, 0.0, 1)),
            emptyList()
        )
        assertTrue(csv.contains("\"BT\nDevice\""))
    }

    @Test
    fun exportToCsv_allRecordTypes() = runBlocking {
        val csv = exportService.exportToCsv(
            listOf(WifiScanRecord("WiFi1", "00:11:22:33:44:55", -50, 2400, 1000L, 39.9, 116.4, 1)),
            listOf(BluetoothScanRecord("BT1", "AA:BB:CC:DD:EE:FF", -70, "LE", 1000L, 39.9, 116.4, 2)),
            listOf(LocationRecord(39.9, 116.4, 50.0, 10.0f, 1000L))
        )
        assertTrue(csv.contains("WiFi1"))
        assertTrue(csv.contains("BT1"))
        assertTrue(csv.contains("39.9"))
        assertTrue(csv.lines().size >= 6)
    }

    @Test
    fun exportToJson_emptyLists_returnsEmptyArrays() = runBlocking {
        val json = exportService.exportToJson(emptyList(), emptyList(), emptyList())
        val root = Json.parseToJsonElement(json).jsonObject
        assertEquals(0, root["wifiRecords"]!!.jsonArray.size)
        assertEquals(0, root["bluetoothRecords"]!!.jsonArray.size)
        assertEquals(0, root["locationRecords"]!!.jsonArray.size)
    }

    @Test
    fun exportToJson_wifiRecords_includesAllFields() = runBlocking {
        val json = exportService.exportToJson(
            listOf(WifiScanRecord("TestWiFi", "00:11:22:33:44:55", -50, 2400, 1000L, 39.9, 116.4, 3)),
            emptyList(), emptyList()
        )
        val wifi = Json.parseToJsonElement(json).jsonObject["wifiRecords"]!!.jsonArray[0].jsonObject
        assertEquals("TestWiFi", wifi["ssid"]!!.jsonPrimitive.content)
        assertEquals("00:11:22:33:44:55", wifi["bssid"]!!.jsonPrimitive.content)
        assertEquals(-50, wifi["signalStrength"]!!.jsonPrimitive.int)
        assertEquals(2400, wifi["frequency"]!!.jsonPrimitive.int)
        assertEquals(1000L, wifi["timestamp"]!!.jsonPrimitive.long)
        assertEquals(39.9, wifi["latitude"]!!.jsonPrimitive.double, 0.001)
        assertEquals(116.4, wifi["longitude"]!!.jsonPrimitive.double, 0.001)
        assertEquals(3, wifi["count"]!!.jsonPrimitive.int)
    }

    @Test
    fun exportToJson_bluetoothRecords_includesAllFields() = runBlocking {
        val json = exportService.exportToJson(
            emptyList(),
            listOf(BluetoothScanRecord("TestBT", "AA:BB:CC:DD:EE:FF", -70, "LE", 1000L, 39.9, 116.4, 2)),
            emptyList()
        )
        val bt = Json.parseToJsonElement(json).jsonObject["bluetoothRecords"]!!.jsonArray[0].jsonObject
        assertEquals("TestBT", bt["name"]!!.jsonPrimitive.content)
        assertEquals("AA:BB:CC:DD:EE:FF", bt["address"]!!.jsonPrimitive.content)
        assertEquals(-70, bt["rssi"]!!.jsonPrimitive.int)
        assertEquals("LE", bt["deviceType"]!!.jsonPrimitive.content)
        assertEquals(2, bt["count"]!!.jsonPrimitive.int)
    }

    @Test
    fun exportToJson_locationRecords_includesAllFields() = runBlocking {
        val json = exportService.exportToJson(
            emptyList(), emptyList(),
            listOf(LocationRecord(39.9, 116.4, 50.0, 10.0f, 1000L))
        )
        val loc = Json.parseToJsonElement(json).jsonObject["locationRecords"]!!.jsonArray[0].jsonObject
        assertEquals(39.9, loc["latitude"]!!.jsonPrimitive.double, 0.001)
        assertEquals(116.4, loc["longitude"]!!.jsonPrimitive.double, 0.001)
        assertEquals(50.0, loc["altitude"]!!.jsonPrimitive.double, 0.001)
        assertEquals(10.0, loc["accuracy"]!!.jsonPrimitive.double, 0.001)
    }

    @Test
    fun exportToJson_specialCharacters() = runBlocking {
        val json = exportService.exportToJson(
            listOf(WifiScanRecord("WiFi \"Office\" & Co.", "00:11:22:33:44:55", -50, 2400, 1000L, 0.0, 0.0, 1)),
            emptyList(), emptyList()
        )
        val ssid = Json.parseToJsonElement(json)
            .jsonObject["wifiRecords"]!!.jsonArray[0].jsonObject["ssid"]!!.jsonPrimitive.content
        assertEquals("WiFi \"Office\" & Co.", ssid)
    }

    @Test
    fun exportToJson_multipleRecords() = runBlocking {
        val json = exportService.exportToJson(
            listOf(
                WifiScanRecord("WiFi1", "00:11:22:33:44:01", -50, 2400, 1000L, 0.0, 0.0, 1),
                WifiScanRecord("WiFi2", "00:11:22:33:44:02", -60, 5000, 2000L, 0.0, 0.0, 2)
            ),
            listOf(BluetoothScanRecord("BT1", "AA:BB:CC:DD:EE:01", -70, "LE", 1000L, 0.0, 0.0, 1)),
            emptyList()
        )
        val root = Json.parseToJsonElement(json).jsonObject
        assertEquals(2, root["wifiRecords"]!!.jsonArray.size)
        assertEquals(1, root["bluetoothRecords"]!!.jsonArray.size)
        assertEquals(0, root["locationRecords"]!!.jsonArray.size)
    }

    @Test
    fun exportToCsv_noHeaderRepeatsForEmptySections() = runBlocking {
        val csv = exportService.exportToCsv(emptyList(), emptyList(), emptyList())
        val wifiHeaderCount = csv.lines().count { it.contains("SSID,BSSID") }
        assertEquals(1, wifiHeaderCount)
    }
}
