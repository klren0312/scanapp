package com.example.scanapp.service

import com.example.scanapp.database.AndroidDatabaseDriver
import java.io.File

actual object PlatformExportController {
    actual suspend fun exportAndShareFile(
        fileName: String,
        content: String,
        mimeType: String
    ): ExportFileResult {
        return runCatching {
            val context = AndroidDatabaseDriver.requireContext()
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val exportFile = File(exportDir, fileName.withTimestamp().sanitizeFileName()).apply {
                writeText(content)
            }

            AndroidExportService(context).shareFile(exportFile.absolutePath)

            ExportFileResult(
                success = true,
                message = "Exported",
                filePath = exportFile.absolutePath
            )
        }.getOrElse {
            ExportFileResult(
                success = false,
                message = it.message ?: it::class.simpleName.orEmpty()
            )
        }
    }

    private fun String.sanitizeFileName(): String {
        return replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun String.withTimestamp(): String {
        val extensionIndex = lastIndexOf('.')
        val timestamp = System.currentTimeMillis()
        return if (extensionIndex > 0) {
            "${substring(0, extensionIndex)}-$timestamp${substring(extensionIndex)}"
        } else {
            "$this-$timestamp"
        }
    }
}
