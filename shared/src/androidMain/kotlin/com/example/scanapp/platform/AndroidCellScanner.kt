package com.example.scanapp.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoTdscdma
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrength
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.scanapp.models.CellScanRecord
import com.example.scanapp.models.cellKeyOf

class AndroidCellScanner(context: Context) {

    private val appContext = context.applicationContext
    private val telephonyManager =
        appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    // On Android Q+, a fresh cell-info snapshot is delivered asynchronously via
    // requestCellInfoUpdate. Reading allCellInfo synchronously right after the
    // request is still empty on most devices, so we await the callback and only
    // fall back to the cached allCellInfo snapshot when the refresh is unavailable
    // or times out.
    fun scanCellInfo(): List<CellScanRecord> {
        if (!hasLocationPermission()) {
            android.util.Log.w("AndroidCellScanner", "Missing location permission for cell scan")
            return emptyList()
        }

        val infos = try {
            requestFreshCellInfo()
        } catch (e: SecurityException) {
            android.util.Log.e("AndroidCellScanner", "SecurityException reading cell info: ${e.message}")
            null
        } catch (e: Exception) {
            android.util.Log.e("AndroidCellScanner", "Cell info read failed: ${e.message}")
            null
        } ?: runCatching { telephonyManager.allCellInfo }.getOrNull() ?: return emptyList()

        val now = System.currentTimeMillis()
        val records = infos.mapNotNull { info ->
            runCatching { toRecord(info, now) }.getOrNull()
        }
        android.util.Log.d(
            "AndroidCellScanner",
            "Cell scan raw=${infos.size}, kept=${records.size}, sdk=${Build.VERSION.SDK_INT}"
        )
        return records
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    // Awaits a fresh cell-info snapshot on Android Q+ and returns it, or null when
    // the async refresh is unavailable/timed out so the caller falls back to allCellInfo.
    // Only overrides onCellInfo (not onError) to stay link-safe on API 29.
    private fun requestFreshCellInfo(): List<CellInfo>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }
        val latch = java.util.concurrent.CountDownLatch(1)
        var snapshot: List<CellInfo>? = null
        return runCatching {
            telephonyManager.requestCellInfoUpdate(
                appContext.mainExecutor,
                object : TelephonyManager.CellInfoCallback() {
                    override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                        snapshot = ArrayList(cellInfo)
                        latch.countDown()
                    }
                }
            )
            if (!latch.await(3L, java.util.concurrent.TimeUnit.SECONDS)) {
                android.util.Log.w("AndroidCellScanner", "requestCellInfoUpdate timed out, using cached snapshot")
                return@runCatching null
            }
            snapshot
        }.getOrElse {
            android.util.Log.w("AndroidCellScanner", "requestCellInfoUpdate error: ${it.message}")
            null
        }
    }

    private data class CellIdentityData(
        val type: String,
        val mcc: Int,
        val mnc: Int,
        val lac: Long,
        val cid: Long
    )

    private fun normInt(v: Int): Int =
        if (v <= 0 || v == Int.MAX_VALUE || v == Int.MIN_VALUE) -1 else v

    private fun normLong(v: Long): Long =
        if (v <= 0L || v == Long.MAX_VALUE || v == Long.MIN_VALUE) -1L else v

    private fun parseMccMnc(mccString: String?, mncString: String?, mcc: Int, mnc: Int): Pair<Int, Int> {
        val parsedMcc = mccString?.toIntOrNull()?.let(::normInt) ?: normInt(mcc)
        val parsedMnc = mncString?.toIntOrNull()?.let(::normInt) ?: normInt(mnc)
        return parsedMcc to parsedMnc
    }

    private fun identity(info: CellInfo): CellIdentityData? {
        // CellInfoNr only exists on API 29+. Avoid a direct `is CellInfoNr` on older devices.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            info.javaClass.name == "android.telephony.CellInfoNr"
        ) {
            return identityNr(info)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            info.javaClass.name == "android.telephony.CellInfoTdscdma"
        ) {
            return identityTdscdma(info)
        }

        return when (info) {
            is CellInfoLte -> {
                val id = info.cellIdentity
                val (mcc, mnc) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    parseMccMnc(id.mccString, id.mncString, id.mcc, id.mnc)
                } else {
                    @Suppress("DEPRECATION")
                    normInt(id.mcc) to normInt(id.mnc)
                }
                CellIdentityData(
                    type = "LTE",
                    mcc = mcc,
                    mnc = mnc,
                    lac = normInt(id.tac).toLong(),
                    cid = normInt(id.ci).toLong()
                )
            }
            is CellInfoWcdma -> {
                val id = info.cellIdentity
                val (mcc, mnc) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    parseMccMnc(id.mccString, id.mncString, id.mcc, id.mnc)
                } else {
                    @Suppress("DEPRECATION")
                    normInt(id.mcc) to normInt(id.mnc)
                }
                CellIdentityData(
                    type = "WCDMA",
                    mcc = mcc,
                    mnc = mnc,
                    lac = normInt(id.lac).toLong(),
                    cid = normInt(id.cid).toLong()
                )
            }
            is CellInfoGsm -> {
                val id = info.cellIdentity
                val (mcc, mnc) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    parseMccMnc(id.mccString, id.mncString, id.mcc, id.mnc)
                } else {
                    @Suppress("DEPRECATION")
                    normInt(id.mcc) to normInt(id.mnc)
                }
                CellIdentityData(
                    type = "GSM",
                    mcc = mcc,
                    mnc = mnc,
                    lac = normInt(id.lac).toLong(),
                    cid = normInt(id.cid).toLong()
                )
            }
            is CellInfoCdma -> {
                val id = info.cellIdentity
                // CDMA has no MCC/MNC; keep networkId/systemId so the record is not dropped.
                CellIdentityData(
                    type = "CDMA",
                    mcc = 0,
                    mnc = 0,
                    lac = normInt(id.networkId).toLong(),
                    cid = normInt(id.systemId).toLong()
                )
            }
            else -> null
        }
    }

    private fun identityNr(info: CellInfo): CellIdentityData? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val id = (info as? CellInfoNr)?.cellIdentity as? android.telephony.CellIdentityNr ?: return null
        val (mcc, mnc) = parseMccMnc(id.mccString, id.mncString, -1, -1)
        // Prefer TAC/NCI; fall back to ARFCN/PCI when the full cell identity is unavailable.
        val lac = normInt(id.tac).toLong().takeIf { it >= 0 } ?: normInt(id.nrarfcn).toLong()
        val cid = normLong(id.nci).takeIf { it >= 0 } ?: normInt(id.pci).toLong()
        return CellIdentityData(
            type = "NR",
            mcc = mcc,
            mnc = mnc,
            lac = lac,
            cid = cid
        )
    }

    private fun identityTdscdma(info: CellInfo): CellIdentityData? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val id = (info as? CellInfoTdscdma)?.cellIdentity ?: return null
        val (mcc, mnc) = parseMccMnc(id.mccString, id.mncString, -1, -1)
        return CellIdentityData(
            type = "TDSCDMA",
            mcc = mcc,
            mnc = mnc,
            lac = normInt(id.lac).toLong(),
            cid = normInt(id.cid).toLong()
        )
    }

    private fun signalDbm(info: CellInfo): Int {
        // Prefer the unified API on Android 11+, otherwise fall back to type-specific getters.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val strength: CellSignalStrength = try {
                info.cellSignalStrength
            } catch (_: Throwable) {
                return typeSpecificSignalDbm(info)
            }
            val dbm = runCatching { strength.dbm }.getOrDefault(Int.MAX_VALUE)
            if (dbm != Int.MAX_VALUE && dbm != Int.MIN_VALUE) return dbm
        }
        return typeSpecificSignalDbm(info)
    }

    private fun typeSpecificSignalDbm(info: CellInfo): Int {
        val dbm = when {
            info is CellInfoLte -> info.cellSignalStrength.dbm
            info is CellInfoWcdma -> info.cellSignalStrength.dbm
            info is CellInfoGsm -> info.cellSignalStrength.dbm
            info is CellInfoCdma -> info.cellSignalStrength.dbm
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                info.javaClass.name == "android.telephony.CellInfoNr" -> {
                ((info as? CellInfoNr)?.cellSignalStrength as? android.telephony.CellSignalStrengthNr)
                    ?.dbm ?: 0
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                info.javaClass.name == "android.telephony.CellInfoTdscdma" -> {
                (info as? CellInfoTdscdma)?.cellSignalStrength?.dbm ?: 0
            }
            else -> 0
        }
        return if (dbm == Int.MAX_VALUE || dbm == Int.MIN_VALUE) 0 else dbm
    }

    private fun operatorName(): String {
        return runCatching { telephonyManager.networkOperatorName ?: "Unknown" }
            .getOrDefault("Unknown")
            .ifBlank { "Unknown" }
    }

    private fun toRecord(info: CellInfo, timestamp: Long): CellScanRecord? {
        val id = identity(info) ?: return null
        // CDMA intentionally uses mcc/mnc = 0. Other networks still require valid MCC/MNC.
        val isCdma = id.type == "CDMA"
        if (!isCdma && (id.mcc <= 0 || id.mnc < 0)) return null
        if (id.lac < 0 || id.cid < 0) return null
        return CellScanRecord(
            cellKey = cellKeyOf(id.type, id.mcc, id.mnc, id.lac, id.cid),
            networkType = id.type,
            operator = operatorName(),
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
