package com.example.androidautonote.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object FileExportHelper {

    /**
     * Creates a .txt file from note content and returns a share Intent.
     */
    fun createShareIntent(
        context: Context,
        title: String,
        content: String,
        createdAt: Long
    ): Intent {
        val fileName = sanitizeFileName(title) + ".txt"
        val fileContent = buildString {
            appendLine(title)
            appendLine("Ngày tạo: ${DateUtils.formatDateTime(createdAt)}")
            appendLine("─".repeat(40))
            appendLine()
            append(content)
        }

        // Write to cache dir (no storage permission needed)
        val cacheDir = File(context.cacheDir, "exports")
        cacheDir.mkdirs()
        val file = File(cacheDir, fileName)
        file.writeText(fileContent, Charsets.UTF_8)

        // Create content URI via FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9\\-_ \\p{L}]"), "")
            .take(50)
            .trim()
            .ifEmpty { "note" }
    }
}
