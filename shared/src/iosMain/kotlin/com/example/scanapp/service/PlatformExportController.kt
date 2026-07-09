package com.example.scanapp.service

actual object PlatformExportController {
    actual suspend fun exportAndShareFile(
        fileName: String,
        content: String,
        mimeType: String
    ): ExportFileResult {
        return ExportFileResult(false, "Export sharing is not available on iOS yet")
    }
}
