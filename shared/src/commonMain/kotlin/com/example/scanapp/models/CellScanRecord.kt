package com.example.scanapp.models

import kotlinx.serialization.Serializable

@Serializable
data class CellScanRecord(
    val id: Long = 0,
    val cellKey: String,
    val networkType: String,
    val operator: String,
    val mcc: Int,
    val mnc: Int,
    val lac: Long,
    val cid: Long,
    val signalStrength: Int,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val count: Int = 1
)

fun cellKeyOf(networkType: String, mcc: Int, mnc: Int, lac: Long, cid: Long): String =
    "$networkType:$mcc:$mnc:$lac:$cid"
