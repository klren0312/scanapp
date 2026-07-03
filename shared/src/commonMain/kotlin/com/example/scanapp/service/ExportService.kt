package com.example.scanapp.service

import com.example.scanapp.models.BluetoothScanRecord
import com.example.scanapp.models.LocationRecord
import com.example.scanapp.models.WifiScanRecord

interface ExportService {
    suspend fun exportToCsv(
        wifiRecords: List<WifiScanRecord>,
        bluetoothRecords: List<BluetoothScanRecord>,
        locationRecords: List<LocationRecord>
    ): String

    suspend fun exportToJson(
        wifiRecords: List<WifiScanRecord>,
        bluetoothRecords: List<BluetoothScanRecord>,
        locationRecords: List<LocationRecord>
    ): String

    suspend fun shareFile(filePath: String)
}
