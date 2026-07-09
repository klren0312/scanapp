package com.example.scanapp.service

expect object PlatformExportController {
    suspend fun exportAndShareFile(
        fileName: String,
        content: String,
        mimeType: String
    ): ExportFileResult
}

data class ExportFileResult(
    val success: Boolean,
    val message: String,
    val filePath: String = ""
)
