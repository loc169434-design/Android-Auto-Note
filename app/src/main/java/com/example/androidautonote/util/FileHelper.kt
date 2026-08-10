package com.example.androidautonote.util

import android.content.Context
import android.util.Log
import java.io.File
import java.util.Calendar
import java.util.Locale

/**
 * Central helper for the AndroidAutoNote file system.
 *
 * Folder: Android/data/com.example.androidautonote/files/AndroidAutoNote/
 *
 * Two fixed files — both use the SAME format, appended on each recording:
 *   raw.txt        — editable master file
 *   fileguidi.txt  — backup, overwritten from raw.txt on every edit-save
 *
 * Entry format:
 *   - Thứ ba, ngày 12-08-2026 lúc 09.30: [content]
 *
 * Entries are stored oldest-first (append). Display reverses for newest-first.
 */
object FileHelper {

    private const val TAG = "FileHelper"
    const val FOLDER_NAME = "AndroidAutoNote"
    const val RAW_FILE    = "raw.txt"
    const val GUIDI_FILE  = "fileguidi.txt"

    private val DAY_NAMES = mapOf(
        Calendar.MONDAY    to "Thứ hai",
        Calendar.TUESDAY   to "Thứ ba",
        Calendar.WEDNESDAY to "Thứ tư",
        Calendar.THURSDAY  to "Thứ năm",
        Calendar.FRIDAY    to "Thứ sáu",
        Calendar.SATURDAY  to "Thứ bảy",
        Calendar.SUNDAY    to "Chủ nhật"
    )

    // Keywords that trigger the NEXT LINE to be masked with *** in display
    val SENSITIVE_KEYWORDS = listOf("mật khẩu", "password", "pass", "mk")

    // ── Directory ─────────────────────────────────────────────────────────────

    fun getNotesDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(base, FOLDER_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getRawFile(context: Context)   = File(getNotesDir(context), RAW_FILE)
    fun getGuidiFile(context: Context) = File(getNotesDir(context), GUIDI_FILE)

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Append a new note entry to BOTH raw.txt and fileguidi.txt.
     *
     * Format: `\n- Thứ ba, ngày 12-08-2026 lúc 09.30: [text]`
     */
    fun appendNote(context: Context, text: String) {
        val entry = buildEntry(text)
        try {
            getRawFile(context).appendText(entry, Charsets.UTF_8)
            getGuidiFile(context).appendText(entry, Charsets.UTF_8)
            Log.d(TAG, "Appended note to $FOLDER_NAME")
        } catch (e: Exception) {
            Log.e(TAG, "appendNote failed", e)
        }
    }

    private fun buildEntry(text: String): String {
        val cal = Calendar.getInstance()
        val dayName = DAY_NAMES[cal.get(Calendar.DAY_OF_WEEK)] ?: "Ngày"
        val day   = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.DAY_OF_MONTH))
        val month = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MONTH) + 1)
        val year  = cal.get(Calendar.YEAR)
        val hour  = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.HOUR_OF_DAY))
        val min   = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MINUTE))
        return "\n- $dayName, ngày $day-$month-$year lúc $hour.$min: $text"
    }

    // ── Read / Parse ──────────────────────────────────────────────────────────

    /**
     * A single parsed note entry.
     */
    data class NoteEntry(
        val header: String,   // e.g. "Thứ ba, ngày 12-08-2026 lúc 09.30"
        val content: String,  // everything after ": "
        val fullLine: String  // original line as stored
    )

    /**
     * Parse fileguidi.txt into a list of NoteEntry objects, newest first.
     */
    fun parseEntries(context: Context): List<NoteEntry> {
        val file = getGuidiFile(context)
        if (!file.exists()) return emptyList()

        return try {
            file.readLines(Charsets.UTF_8)
                .asSequence()
                .map { it.trim() }
                .filter { it.startsWith("- Thứ") || it.startsWith("- Chủ") }
                .mapNotNull { line ->
                    // Format: "- Thứ ba, ngày DD-MM-YYYY lúc HH.mm: content"
                    val colonIdx = line.indexOf(": ")
                    if (colonIdx == -1) return@mapNotNull null
                    val header  = line.substring(2, colonIdx).trim() // strip leading "- "
                    val content = line.substring(colonIdx + 2).trim()
                    NoteEntry(header, content, line)
                }
                .toList()
                .reversed()  // newest first
        } catch (e: Exception) {
            Log.e(TAG, "parseEntries failed", e)
            emptyList()
        }
    }

    // ── Raw file edit ─────────────────────────────────────────────────────────

    fun readRawFile(context: Context): String {
        val f = getRawFile(context)
        return if (f.exists()) f.readText(Charsets.UTF_8) else ""
    }

    /**
     * Save edited content to raw.txt AND overwrite fileguidi.txt with the same content.
     * Returns error message on validation failure, null on success.
     */
    fun saveEditedRaw(context: Context, original: String, edited: String): String? {
        val err = validateEdit(original, edited)
        if (err != null) return err
        try {
            getRawFile(context).writeText(edited, Charsets.UTF_8)
            getGuidiFile(context).writeText(edited, Charsets.UTF_8)
        } catch (e: Exception) {
            return "Lỗi ghi file: ${e.message}"
        }
        return null
    }

    /**
     * Validate edit constraints:
     * - Date header lines (starting with "- Thứ" / "- Chủ") must not be removed or modified
     * - No more than 2 lines removed in one edit session
     */
    private fun validateEdit(original: String, edited: String): String? {
        val origLines = original.lines()
        val editLines = edited.lines()

        val isDateLine = { l: String -> l.trimStart().startsWith("- Thứ") || l.trimStart().startsWith("- Chủ") }
        val origHeaders = origLines.filter(isDateLine)
        val editHeaders = editLines.filter(isDateLine)

        if (origHeaders.size != editHeaders.size || origHeaders.zip(editHeaders).any { (a, b) -> a != b }) {
            return "Không được xóa hoặc sửa dòng ngày tháng cố định"
        }

        val linesRemoved = origLines.size - editLines.size
        if (linesRemoved >= 3) {
            return "Không thể xóa $linesRemoved dòng cùng lúc (tối đa 2 dòng mỗi lần)"
        }
        return null
    }

    // ── Password masking (display only) ───────────────────────────────────────

    /**
     * Apply line-by-line masking: if line N contains a sensitive keyword,
     * line N+1 is displayed as "***" (actual file content is never changed).
     */
    fun maskSensitive(lines: List<String>): List<String> {
        var maskNext = false
        return lines.map { line ->
            if (maskNext) {
                maskNext = false
                "***"
            } else {
                val lower = line.lowercase()
                maskNext = SENSITIVE_KEYWORDS.any { lower.contains(it) }
                line
            }
        }
    }

    // ── Compatibility: read fileguidi (for Gemini sharing) ───────────────────

    fun readGuidiFile(context: Context): String? {
        val f = getGuidiFile(context)
        return if (f.exists() && f.length() > 0) {
            try { f.readText(Charsets.UTF_8) } catch (e: Exception) { null }
        } else null
    }
}
