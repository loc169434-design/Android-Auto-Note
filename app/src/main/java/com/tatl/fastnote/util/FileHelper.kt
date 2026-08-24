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

    // ── Regex Header Tiền Tố Ngày Tháng ──────────────────────────────────────
    val DATE_HEADER_REGEX = Regex(
        """^-\s*(?:Thứ\s+[a-zA-Z\p{L}]+|Chủ\s+nhật|Ngày)(?:,\s*ngày|\s+ngày)?\s*\d{1,2}[-/]\d{1,2}[-/]\d{4}\s*lúc\s*\d{1,2}[\.:]\d{2}\s*:""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Trích xuất chính xác tiền tố ngày tháng từ đầu dòng (bao gồm dấu ':').
     * Trả về null nếu dòng không phải là dòng bắt đầu ngày tháng.
     */
    fun extractDateHeader(line: String): String? {
        val trimmed = line.trimStart()
        val match = DATE_HEADER_REGEX.find(trimmed)
        return match?.value?.trim()
    }

    // ── Sample Data cho môi trường không tiện ghi âm ───────────────────────────
    fun ensureSampleData(context: Context) {
        val rawFile = getRawFile(context)
        val guidiFile = getGuidiFile(context)
        if (!rawFile.exists() || rawFile.length() == 0L || !guidiFile.exists() || guidiFile.length() == 0L) {
            val sampleText = buildSampleNotes()
            try {
                rawFile.writeText(sampleText, Charsets.UTF_8)
                guidiFile.writeText(sampleText, Charsets.UTF_8)
                Log.d(TAG, "Initialized sample notes data")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize sample notes", e)
            }
        }
    }

    private fun buildSampleNotes(): String {
        val cal = Calendar.getInstance()
        fun formatDate(c: Calendar, hour: Int, minute: Int): String {
            val dayName = DAY_NAMES[c.get(Calendar.DAY_OF_WEEK)] ?: "Ngày"
            val day = String.format(Locale.getDefault(), "%02d", c.get(Calendar.DAY_OF_MONTH))
            val month = String.format(Locale.getDefault(), "%02d", c.get(Calendar.MONTH) + 1)
            val year = c.get(Calendar.YEAR)
            val h = String.format(Locale.getDefault(), "%02d", hour)
            val m = String.format(Locale.getDefault(), "%02d", minute)
            return "- $dayName, ngày $day-$month-$year lúc $h.$m:"
        }

        val header1 = formatDate(cal, 8, 30)
        val header2 = formatDate(cal, 10, 15)

        val calPrev = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }
        val header3 = formatDate(calPrev, 16, 45)

        return "$header1 Họp giao ban đầu tuần, thảo luận về kế hoạch triển khai tính năng AI và đồng bộ đám mây.\n\n" +
                "$header2 **Ý TƯỞNG PHÁT TRIỂN:** Tối ưu hóa bộ nhận diện Regex cho ghi chú, hỗ trợ xóa nội dung linh hoạt và tự động bảo vệ tiền tố ngày tháng.\n\n" +
                "$header3 Mua sách và chuẩn bị tài liệu nghiên cứu Jetpack Compose Canvas."
    }

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
     * Supports single-line and multi-line notes.
     */
    fun parseEntries(context: Context): List<NoteEntry> {
        ensureSampleData(context)
        val file = getGuidiFile(context)
        if (!file.exists()) return emptyList()

        return try {
            val lines = file.readLines(Charsets.UTF_8)
            val entries = mutableListOf<NoteEntry>()
            var currentHeader: String? = null
            val currentContent = StringBuilder()
            val currentFull = StringBuilder()

            fun flush() {
                val h = currentHeader ?: ""
                val c = currentContent.toString().trim()
                val f = currentFull.toString().trim()
                if (h.isNotEmpty() || c.isNotEmpty() || f.isNotEmpty()) {
                    entries.add(NoteEntry(h, c, f))
                }
                currentHeader = null
                currentContent.clear()
                currentFull.clear()
            }

            for (line in lines) {
                val trimmed = line.trim()
                val match = DATE_HEADER_REGEX.find(trimmed)
                if (match != null) {
                    flush()
                    val matchEnd = match.range.last + 1
                    val rawHeader = match.value.trimStart('-', ' ').trimEnd(':').trim()
                    currentHeader = rawHeader
                    val contentAfter = trimmed.substring(matchEnd).trimStart()
                    if (contentAfter.isNotEmpty()) {
                        currentContent.append(contentAfter)
                    }
                    currentFull.append(line)
                } else {
                    if (trimmed.isNotEmpty()) {
                        if (currentContent.isNotEmpty()) currentContent.append("\n")
                        currentContent.append(line)
                    }
                    if (currentFull.isNotEmpty()) currentFull.append("\n")
                    currentFull.append(line)
                }
            }
            flush()
            entries.reversed()  // newest first
        } catch (e: Exception) {
            Log.e(TAG, "parseEntries failed", e)
            emptyList()
        }
    }

    // ── Raw file edit ─────────────────────────────────────────────────────────

    fun readRawFile(context: Context): String {
        ensureSampleData(context)
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
     * Kiểm tra xem tất cả header gốc (origHeaders) có còn tồn tại đầy đủ theo đúng thứ tự
     * trong danh sách header mới (newHeaders) hay không (cho phép chèn/thêm header mới).
     */
    fun isValidHeaderPreservation(origHeaders: List<String>, newHeaders: List<String>): Boolean {
        if (origHeaders.isEmpty()) return true
        if (newHeaders.size < origHeaders.size) return false

        var origIdx = 0
        for (newH in newHeaders) {
            if (newH == origHeaders[origIdx]) {
                origIdx++
                if (origIdx == origHeaders.size) return true
            }
        }
        return origIdx == origHeaders.size
    }

    /**
     * Validate edit constraints:
     * - Date header lines (bắt bằng DATE_HEADER_REGEX) không được bị xóa hoặc sửa đổi nội dung bên trong tiền tố cũ
     * - Người dùng được phép tự gõ/chèn thêm header ngày tháng mới
     * - Chỉ phần tiền tố (trước và gồm dấu ':') được bảo vệ; nội dung sau dấu ':' được tự do xóa/sửa
     * - Nếu ban đầu chưa có header nào (file trống/note tự do) -> cho phép lưu tự do
     */
    private fun validateEdit(original: String, edited: String): String? {
        val origLines = original.lines()
        val editLines = edited.lines()

        val origHeaders = origLines.mapNotNull { extractDateHeader(it) }
        val editHeaders = editLines.mapNotNull { extractDateHeader(it) }

        if (!isValidHeaderPreservation(origHeaders, editHeaders)) {
            return "Không được xóa hoặc sửa dòng ngày tháng cố định"
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

    /**
     * Xuất tệp tin sạch đã lọc bảo mật để gửi sang Google Gemini (V38 Phần 6):
     * - Tiếng Việt: File_gui_di_(Da_loc_bao_mat).txt
     * - Quốc tế: Shared_File_(Privacy_Protected).txt
     */
    fun getAiSharedFile(context: Context): File {
        val fileName = com.tatl.fastnote.data.user.LanguageManager.getSharedFileName()
        val cleanFile = File(getNotesDir(context), fileName)
        val rawText = readRawFile(context).ifBlank { readGuidiFile(context) ?: "" }
        if (rawText.isNotBlank()) {
            val lines = rawText.lines()
            val maskedLines = maskSensitive(lines)
            cleanFile.writeText(maskedLines.joinToString("\n"), Charsets.UTF_8)
        }
        return cleanFile
    }
}
