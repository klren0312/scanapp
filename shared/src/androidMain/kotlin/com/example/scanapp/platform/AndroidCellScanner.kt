package com.example.scanapp.platform

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrength
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.example.scanapp.models.CellScanRecord
import com.example.scanapp.models.cellKeyOf

class AndroidCellScanner(private val context: Context) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    @RequiresApi(Build.VERSION_CODES.R)
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun scanCellInfo(): List<CellScanRecord> {
        val infos = telephonyManager.allCellInfo ?: return emptyList()
        val now = System.currentTimeMillis()
        return infos.mapNotNull { toRecord(it, now) }
    }

    private data class CellIdentityData(
        val type: String, val mcc: Int, val mnc: Int, val lac: Long, val cid: Long
    )

    // 归一化：未知值（<=0 或平台哨兵 Integer.MAX_VALUE）统一记为 -1，最终被 toRecord 过滤丢弃
    private fun norm(v: Int): Int = if (v <= 0 || v == Int.MAX_VALUE) -1 else v

    private fun identity(info: CellInfo): CellIdentityData? {
        // CellInfoNr / CellIdentityNr only exist on API 29+. Reference them only through a
        // string check + a @TargetApi(Q) helper so the class is never loaded on older devices
        // (a direct `is CellInfoNr` would throw NoClassDefFoundError at runtime on API < 29).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            info.javaClass.name == "android.telephony.CellInfoNr"
        ) {
            return identityNr(info)
        }
        return when (info) {
            is CellInfoLte -> {
                val id = info.cellIdentity as? android.telephony.CellIdentityLte ?: return null
                CellIdentityData(
                    "LTE",
                    norm(id.mcc ?: -1),
                    norm(id.mnc ?: -1),
                    norm(id.tac ?: -1).toLong(),
                    norm(id.ci ?: -1).toLong()
                )
            }
            is CellInfoWcdma -> {
                val id = info.cellIdentity as? android.telephony.CellIdentityWcdma ?: return null
                CellIdentityData(
                    "WCDMA",
                    norm(id.mcc ?: -1),
                    norm(id.mnc ?: -1),
                    norm(id.lac ?: -1).toLong(),
                    norm(id.cid ?: -1).toLong()
                )
            }
            is CellInfoGsm -> {
                val id = info.cellIdentity as? android.telephony.CellIdentityGsm ?: return null
                CellIdentityData(
                    "GSM",
                    norm(id.mcc ?: -1),
                    norm(id.mnc ?: -1),
                    norm(id.lac ?: -1).toLong(),
                    norm(id.cid ?: -1).toLong()
                )
            }
            is CellInfoCdma -> {
                val id = info.cellIdentity as? android.telephony.CellIdentityCdma ?: return null
                CellIdentityData("CDMA", -1, -1, norm(id.networkId).toLong(), norm(id.systemId).toLong())
            }
            else -> null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun identityNr(info: CellInfo): CellIdentityData? {
        val id = (info as? CellInfoNr)?.cellIdentity as? android.telephony.CellIdentityNr ?: return null
        return CellIdentityData(
            "NR",
            norm(id.mccString?.toIntOrNull() ?: -1),
            norm(id.mncString?.toIntOrNull() ?: -1),
            norm(id.nrarfcn).toLong(),
            norm(id.pci).toLong()
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun signalDbm(info: CellInfo): Int {
        val strength: CellSignalStrength = try {
            info.cellSignalStrength
        } catch (_: Throwable) {
            return 0
        }
        return runCatching { strength.dbm }.getOrDefault(0)
    }

    private fun operatorName(info: CellInfo): String {
        return runCatching { telephonyManager.networkOperatorName ?: "Unknown" }
            .getOrDefault("Unknown")
            .ifBlank { "Unknown" }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun toRecord(info: CellInfo, timestamp: Long): CellScanRecord? {
        val id = identity(info) ?: return null
        if (id.mcc <= 0 || id.mnc <= 0 || id.lac < 0 || id.cid < 0) return null
        return CellScanRecord(
            cellKey = cellKeyOf(id.type, id.mcc, id.mnc, id.lac, id.cid),
            networkType = id.type,
            operator = operatorName(info),
            mcc = id.mcc,
            mnc = id.mnc,
            lac = id.lac,
            cid = id.cid,
            signalStrength = signalDbm(info),
            timestamp = timestamp,
            latitude = 0.0,
            longitude = 0.0,
            count = 1
        )
    }
}
