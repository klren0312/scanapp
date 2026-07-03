package com.example.scanapp.service

import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.LocationRecord
import com.example.scanapp.models.WifiScanRecord

class ExportServiceImpl : ExportService {

    override suspend fun exportToCsv(
        wifiRecords: List<WifiScanRecord>,
        bluetoothRecords: List<BluetoothScanRecord>,
        locationRecords: List<LocationRecord>
    ): String {
        val sb = StringBuilder()

        sb.appendLine("WiFi Records")
        sb.appendLine("SSID,BSSID,Signal Strength,Frequency,Timestamp,Latitude,Longitude,Count")
        wifiRecords.forEach { record ->
            sb.appendLine(
                "${escapeCsv(record.ssid)},${escapeCsv(record.bssid)}," +
                        "${record.signalStrength},${record.frequency},${record.timestamp}," +
                        "${record.latitude},${record.longitude},${record.count}"
            )
        }

        sb.appendLine()

        sb.appendLine("Bluetooth Records")
        sb.appendLine("Name,Address,RSSI,Device Type,Timestamp,Latitude,Longitude,Count")
        bluetoothRecords.forEach { record ->
            sb.appendLine(
                "${escapeCsv(record.name)},${escapeCsv(record.address)}," +
                        "${record.rssi},${escapeCsv(record.deviceType)},${record.timestamp}," +
                        "${record.latitude},${record.longitude},${record.count}"
            )
        }

        sb.appendLine()

        sb.appendLine("Location Records")
        sb.appendLine("Latitude,Longitude,Altitude,Accuracy,Timestamp")
        locationRecords.forEach { record ->
            sb.appendLine(
                "${record.latitude},${record.longitude},${record.altitude}," +
                        "${record.accuracy},${record.timestamp}"
            )
        }

        return sb.toString()
    }

    override suspend fun exportToJson(
        wifiRecords: List<WifiScanRecord>,
        bluetoothRecords: List<BluetoothScanRecord>,
        locationRecords: List<LocationRecord>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"wifiRecords\": [")
        wifiRecords.forEachIndexed { index, record ->
            sb.appendLine("    ${wifiRecordToJson(record)}${if (index < wifiRecords.lastIndex) "," else ""}")
        }
        sb.appendLine("  ],")
        sb.appendLine("  \"bluetoothRecords\": [")
        bluetoothRecords.forEachIndexed { index, record ->
            sb.appendLine("    ${bluetoothRecordToJson(record)}${if (index < bluetoothRecords.lastIndex) "," else ""}")
        }
        sb.appendLine("  ],")
        sb.appendLine("  \"locationRecords\": [")
        locationRecords.forEachIndexed { index, record ->
            sb.appendLine("    ${locationRecordToJson(record)}${if (index < locationRecords.lastIndex) "," else ""}")
        }
        sb.appendLine("  ]")
        sb.appendLine("}")
        return sb.toString()
    }

    override suspend fun shareFile(filePath: String) {
        // 平台特定实现将在 expect/actual 机制中完成
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun wifiRecordToJson(record: WifiScanRecord): String {
        return "{" +
                "\"ssid\": \"${escapeJson(record.ssid)}\"," +
                "\"bssid\": \"${escapeJson(record.bssid)}\"," +
                "\"signalStrength\": ${record.signalStrength}," +
                "\"frequency\": ${record.frequency}," +
                "\"timestamp\": ${record.timestamp}," +
                "\"latitude\": ${record.latitude}," +
                "\"longitude\": ${record.longitude}," +
                "\"count\": ${record.count}" +
                "}"
    }

    private fun bluetoothRecordToJson(record: BluetoothScanRecord): String {
        return "{" +
                "\"name\": \"${escapeJson(record.name)}\"," +
                "\"address\": \"${escapeJson(record.address)}\"," +
                "\"rssi\": ${record.rssi}," +
                "\"deviceType\": \"${escapeJson(record.deviceType)}\"," +
                "\"timestamp\": ${record.timestamp}," +
                "\"latitude\": ${record.latitude}," +
                "\"longitude\": ${record.longitude}," +
                "\"count\": ${record.count}" +
                "}"
    }

    private fun locationRecordToJson(record: LocationRecord): String {
        return "{" +
                "\"latitude\": ${record.latitude}," +
                "\"longitude\": ${record.longitude}," +
                "\"altitude\": ${record.altitude}," +
                "\"accuracy\": ${record.accuracy}," +
                "\"timestamp\": ${record.timestamp}" +
                "}"
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
