package com.example.scanapp.service

import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.LocationRecord
import com.example.scanapp.models.WifiScanRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { prettyPrint = true }

@Serializable
private data class ExportData(
    val wifiRecords: List<WifiScanRecord>,
    val bluetoothRecords: List<BluetoothScanRecord>,
    val locationRecords: List<LocationRecord>
)

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
        return json.encodeToString(
            ExportData(wifiRecords, bluetoothRecords, locationRecords)
        )
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
}
