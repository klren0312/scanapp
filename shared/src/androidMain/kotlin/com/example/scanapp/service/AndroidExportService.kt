package com.example.scanapp.service

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.LocationRecord
import com.example.scanapp.models.WifiScanRecord
import java.io.File

class AndroidExportService(private val context: Context) : ExportService {

    private val exportServiceImpl = ExportServiceImpl()

    override suspend fun exportToCsv(
        wifiRecords: List<WifiScanRecord>,
        bluetoothRecords: List<BluetoothScanRecord>,
        locationRecords: List<LocationRecord>
    ): String {
        return exportServiceImpl.exportToCsv(wifiRecords, bluetoothRecords, locationRecords)
    }

    override suspend fun exportToJson(
        wifiRecords: List<WifiScanRecord>,
        bluetoothRecords: List<BluetoothScanRecord>,
        locationRecords: List<LocationRecord>
    ): String {
        return exportServiceImpl.exportToJson(wifiRecords, bluetoothRecords, locationRecords)
    }

    override suspend fun shareFile(filePath: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(filePath)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share file").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    private fun getMimeType(filePath: String): String {
        return when {
            filePath.endsWith(".csv", ignoreCase = true) -> "text/csv"
            filePath.endsWith(".json", ignoreCase = true) -> "application/json"
            else -> "application/octet-stream"
        }
    }
}
