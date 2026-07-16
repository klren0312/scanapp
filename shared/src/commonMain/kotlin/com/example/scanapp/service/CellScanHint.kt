package com.example.scanapp.service

// Shared, platform-neutral explanation for why the Cell (base station) count may be
// zero. Returns an empty string when the count is positive or scanning is irrelevant.
fun cellReadinessHint(cellCount: Long): String {
    if (cellCount > 0L) return ""
    return when (getCellScanReadiness()) {
        CellScanReadiness.MISSING_PERMISSION ->
            "Cell (base station) needs location permission. Grant it, then restart scanning."
        CellScanReadiness.UNSUPPORTED ->
            "Cell (base station) scanning is not available on this platform."
        CellScanReadiness.READY ->
            "No cell towers detected yet. Move outdoors or wait a few cycles."
    }
}
