package com.tatl.fastnote.util

import android.content.Context
import android.util.Log
import java.io.File
import java.util.Calendar
import java.util.Locale

/**
 * Central helper for the AndroidAutoNote file system.
 *
 * Folder: Android/data/com.tatl.fastnote/files/AndroidAutoNote/
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
    const val GUIDI_FILE  = "ghichu_clean.txt"

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
        return "\n\n- $dayName, ngày $day-$month-$year lúc $hour.$min: $text"
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
     * - Only the TIMESTAMP part (before ": ") is protected; content after ": " can be edited freely
     * - No more than 4 lines removed in one edit session
     */
    private fun validateEdit(original: String, edited: String): String? {
        val origLines = original.lines()
        val editLines = edited.lines()

        val isDateLine = { l: String -> l.trimStart().startsWith("- Thứ") || l.trimStart().startsWith("- Chủ") }
        // Chi lay phan truoc ": " de so sanh — cho phep sua content sau dau hai cham
        val headerOnly = { l: String ->
            val idx = l.indexOf(": ")
            if (idx != -1) l.substring(0, idx) else l
        }
        val origHeaders = origLines.filter(isDateLine).map(headerOnly)
        val editHeaders = editLines.filter(isDateLine).map(headerOnly)

        if (origHeaders.size != editHeaders.size || origHeaders.zip(editHeaders).any { (a, b) -> a != b }) {
            return "Không được xóa hoặc sửa dòng ngày tháng cố định"
        }

        val linesRemoved = origLines.size - editLines.size
        if (linesRemoved >= 5) {
            return "Không thể xóa $linesRemoved dòng cùng lúc (tối đa 4 dòng mỗi lần)"
        }
        return null
    }

    // ── Sensitive data masking (display only) ─────────────────────────────────

    /**
     * Mask sensitive data in a list of lines (display only, file never changed).
     *
     * Rules:
     *  1. CCCD/CMND: 12 or 9 consecutive digits → ***
     *  2. Password keywords (mk:, pass:, password:, mat khau:, ma pin:) → mask value after ':'
     *  3. Bank account (STK:, tai khoan:) 8-16 digits → ***
     *  4. Visa/Master card pattern (4 groups of 4 digits) → ***
     *  5. VN phone number (10 digits starting 0[3-9]) → ***
     *  6. Email addresses → ***
     */
    fun maskSensitive(lines: List<String>): List<String> {
        return lines.map { line -> maskLine(line) }
    }

    private fun maskLine(line: String): String {
        var result = line

        // Rule 1: CCCD (12 digits) hoac CMND (9 digits) — standalone
        result = result.replace(Regex("""\b(\d{12}|\d{9})\b"""), "***")

        // Rule 2: Sau keyword mat khau / pass / mk / ma pin → che gia tri
        result = result.replace(
            Regex("""(?i)(mk|pass(?:word)?|m[aậ]t\s*kh[aẩ]u|m[aã]\s*pin)\s*:\s*\S+""")
        ) { mr -> "${mr.groupValues[1]}: ***" }

        // Rule 3: STK / tai khoan kem theo 8-16 so
        result = result.replace(
            Regex("""(?i)(stk|t[aà]i\s*kho[aả]n)\s*:\s*(\d{8,16})""")
        ) { mr -> "${mr.groupValues[1]}: ***" }

        // Rule 4: So the Visa/Master (dddd dddd dddd dddd hoac lien tuc 16 chu so)
        result = result.replace(
            Regex("""\b(\d{4}[\s\-]?\d{4}[\s\-]?\d{4}[\s\-]?\d{4})\b"""), "***"
        )

        // Rule 5: SĐT Việt Nam (10 chu so bat dau bang 0[3-9])
        result = result.replace(
            Regex("""\b(0[3-9]\d{8})\b"""), "***"
        )

        // Rule 6: Email
        result = result.replace(
            Regex("""[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}"""), "***"
        )

        return result
    }

    // ── Compatibility: read fileguidi (for Gemini sharing) ───────────────────

    fun readGuidiFile(context: Context): String? {
        val f = getGuidiFile(context)
        return if (f.exists() && f.length() > 0) {
            try { f.readText(Charsets.UTF_8) } catch (e: Exception) { null }
        } else null
    }
}
