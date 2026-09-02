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
        """^-\s*(?:Thứ\s+[a-zA-Z\p{L}]+|Chủ\s+nhật|Ngày)(?:,\s*ngày|\s+ngày|,)?\s*\d{1,2}[-/]\d{1,2}[-/]\d{4}(?:\s*lúc|\s+)\s*\d{1,2}[\.:]\d{2}\s*:""",
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

    // ── Sensitive data masking (Regex Guard) ──────────────────────────────────

    // ── Sensitive data masking (Màng Lọc Bảo Mật 2 Lớp Cho File Gửi AI) ──────

    // 0. Cặp đánh dấu người dùng tự bảo vệ: zz...zz (case-insensitive)
    private val ZZ_SENSITIVE_REGEX = Regex(
        """(?i)zz[\s\S]*?zz"""
    )

    // Lớp 1: Số nhạy cảm
    // 1.1 Thẻ ngân hàng (16 số) / CCCD (12 số) / Số tài khoản dài (12 - 19 chữ số)
    private val BANK_CARD_LONG_NUM_REGEX = Regex(
        """(?<!\d)\d(?:[\s\-]*\d){11,18}(?!\d)"""
    )

    // 1.2 Số điện thoại (10 số) / CMND cũ (9 số) / Đầu số quốc tế (9 - 11 chữ số)
    private val PHONE_SHORT_NUM_REGEX = Regex(
        """(?<!\d)\d(?:[\s\-]*\d){8,10}(?!\d)"""
    )

    // 1.3 Mật khẩu & Từ khóa xác thực
    private val PASSWORD_REGEX = Regex(
        """(?i)(mk|pass(?:word)?|m[aậ]t\s*kh[aẩ]u)\s*[:=l\u00e0\s\-]+\s*([^\s,;]+)"""
    )

    // 1.4 Mã OTP / PIN (4 - 6 số)
    private val OTP_PIN_REGEX = Regex(
        """(?i)(otp|m[aã]\s*(?:pin|x[aá]c\s*nh[aậ]n|otp)|pin)\s*[:=l\u00e0\s\-]+\s*(\d{4,6})"""
    )

    // 1.5 API Key / Hash / Token / Secret Key (>= 20 ký tự)
    private val SECRET_TOKEN_REGEX = Regex(
        """(?<![a-zA-Z0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z0-9_\-]{20,}(?![a-zA-Z0-9])"""
    )

    // 1.6 Địa chỉ Email
    private val EMAIL_REGEX = Regex(
        """[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}"""
    )

    // ── Lớp 2: Từ khóa nhạy cảm đa ngôn ngữ (VN, EN, JA, DE, RU) ─────────────
    // Cứ xuất hiện bất kỳ từ khóa nào dưới đây trong câu ghi chú -> XÓA SẠCH TOÀN BỘ DÒNG ĐÓ
    private val SENSITIVE_LINE_KEYWORDS_REGEX = Regex(
        """(?i)(""" +
        // --- 1. TIẾNG VIỆT, TIẾNG ANH, TIẾNG ĐỨC, TIẾNG NGA (Có ranh giới từ) ---
        """(?<![\p{L}\p{N}])(?:""" +
        """m[aậ]t\s*kh[aẩ]u|mat\s*khau|m[aậ]t\s*m[aã]|mat\s*ma|mk|""" +
        """m[aã]\s*pin|ma\s*pin|m[aã]\s*puk|ma\s*puk|m[aã]\s*otp|ma\s*otp|""" +
        """m[aã]\s*x[aá]c\s*th[uự]c|ma\s*xac\s*thuc|m[aã]\s*x[aá]c\s*nh[aậ]n|ma\s*xac\s*nhan|""" +
        """m[aã]\s*b[aả]o\s*m[aậ]t|ma\s*bao\s*mat|m[aã]\s*b[aả]o\s*v[eệ]|ma\s*bao\s*ve|""" +
        """t[aà]i\s*kho[aả]n\s*ng[aâ]n\s*h[aà]ng|tai\s*khoan\s*ngan\s*hang|""" +
        """s[oố]\s*t[aà]i\s*kho[aả]n|so\s*tai\s*khoan|stk|s[oố]\s*th[eẻ]|so\s*the|""" +
        """m[aã]|ma|""" +
        """password|passwd|passcode|pass|pwd|pw|""" +
        """pin\s*code|pin|puk\s*code|puk|otp\s*code|otp|""" +
        """secret\s*key|secret|auth\s*code|authentication\s*code|verification\s*code|security\s*code|""" +
        """access\s*code|credentials|login\s*info|2fa|mfa|""" +
        """passwort|kennwort|geheimzahl|sicherheitscode|bestätigungscode|verifizierungscode|einmalpasswort|zugangscode|geheimschlüssel|""" +
        """пароль|пар|пвд|пин-код|пин\s*код|пин|пук-код|пук\s*код|пук|отп|одноразовый\s*пароль|""" +
        """код\s*подтверждения|код\s*проверки|код\s*безопасности|секретный\s*код|код\s*доступа""" +
        """)(?![\p{L}\p{N}])|""" +
        // --- 2. TIẾNG NHẬT (Không dùng khoảng trắng giữa từ và trợ từ ngữ pháp) ---
        """パスワード|パス|暗証番号|暗号|認証コード|確認コード|ワンタイムパスワード|合言葉|セキュリティコード|PINコード|PUKコード|秘密鍵""" +
        """)"""
    )

    /**
     * Kiểm tra xem một đoạn văn bản ghi chú có chứa từ khóa nhạy cảm (Lớp 2) hay không.
     */
    fun containsSensitiveKeyword(text: String): Boolean {
        val trimmed = text.trim()
        val match = DATE_HEADER_REGEX.find(trimmed)
        val contentToCheck = if (match != null) {
            trimmed.substring(match.range.last + 1).trim()
        } else {
            trimmed
        }
        if (contentToCheck.isBlank()) return false
        return SENSITIVE_LINE_KEYWORDS_REGEX.containsMatchIn(contentToCheck)
    }

    /**
     * Lọc bảo mật cho danh sách dòng văn bản gửi đi cho AI:
     * - Lớp 2: Luôn kiểm tra và XÓA TOÀN BỘ DÒNG chứa từ khóa nhạy cảm (mk, pass, mật khẩu, pin, otp, v.v.).
     * - Lớp 1: Che các số 9, 10, 12, 16 chữ số, email, token trên các dòng còn lại thành *** (có thể bỏ qua nếu bypassLayer1 = true).
     */
    fun filterForAiSharing(lines: List<String>, bypassLayer1: Boolean = false): List<String> {
        val result = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Lớp 2: Kiểm tra từ khóa nhạy cảm -> Nếu có thì XÓA TOÀN BỘ DÒNG NÀY
            if (containsSensitiveKeyword(trimmed)) {
                continue
            }

            if (bypassLayer1) {
                // Mở khóa ẩn: Bỏ qua che số, giữ nguyên dòng nguyên vẹn
                result.add(line)
            } else {
                // Lớp 1: Che các số nhạy cảm (9, 10, 12, 16 số), email, token còn lại
                val maskedLine = maskLine(line)
                if (maskedLine.isNotBlank()) {
                    result.add(maskedLine)
                }
            }
        }
        return result
    }

    /**
     * Mask sensitive data in a list of lines (display only, file never changed).
     */
    fun maskSensitive(lines: List<String>): List<String> {
        return lines.map { line -> maskLine(line) }
    }

    private fun maskLine(line: String): String {
        val trimmed = line.trim()
        val match = DATE_HEADER_REGEX.find(trimmed)
        return if (match != null) {
            val headerEnd = match.range.last + 1
            val headerPart = trimmed.substring(0, headerEnd)
            val contentPart = trimmed.substring(headerEnd)
            val maskedContent = maskContent(contentPart)
            val leadingWs = line.takeWhile { it.isWhitespace() }
            "$leadingWs$headerPart$maskedContent"
        } else {
            maskContent(line)
        }
    }

    /**
     * Áp dụng toàn bộ bộ lọc Regex Guard lên nội dung text thuần túy (Lớp 1)
     */
    fun maskContent(text: String): String {
        var result = text

        // Rule 0: Che toàn bộ nội dung nằm giữa cặp 'zz' do người dùng chủ động đánh dấu
        result = result.replace(ZZ_SENSITIVE_REGEX, "***")

        // Rule 1: Thẻ ngân hàng 16 số / CCCD 12 số / Số tài khoản dài (12 - 19 chữ số) -> ***
        result = result.replace(BANK_CARD_LONG_NUM_REGEX, "***")

        // Rule 2: Số điện thoại 10 số / CMND cũ 9 số (9 - 11 chữ số) -> ***
        result = result.replace(PHONE_SHORT_NUM_REGEX, "***")

        // Rule 3: Mật khẩu & Từ khóa xác thực -> giữ từ khóa và dấu nối, che giá trị thành ***
        result = result.replace(PASSWORD_REGEX) { mr ->
            val fullMatch = mr.value
            val valToMask = mr.groupValues[2]
            val prefix = fullMatch.substringBeforeLast(valToMask)
            "${prefix}***"
        }

        // Rule 4: Mã OTP / PIN -> giữ từ khóa và dấu nối, che số thành ***
        result = result.replace(OTP_PIN_REGEX) { mr ->
            val fullMatch = mr.value
            val valToMask = mr.groupValues[2]
            val prefix = fullMatch.substringBeforeLast(valToMask)
            "${prefix}***"
        }

        // Rule 5: API Key / Hash / Token / Secret Key -> ***
        result = result.replace(SECRET_TOKEN_REGEX, "***")

        // Rule 6: Email -> ***
        result = result.replace(EMAIL_REGEX, "***")

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
     * Xuất tệp tin sạch đã lọc bảo mật 2 lớp để gửi sang Google Gemini (V38 Phần 6):
     * - Tiếng Việt: File_gui_di_(Da_loc_bao_mat).txt
     * - Quốc tế: Shared_File_(Privacy_Protected).txt
     */
    fun getAiSharedFile(context: Context): File {
        val fileName = com.tatl.fastnote.data.user.LanguageManager.getSharedFileName()
        val cleanFile = File(getNotesDir(context), fileName)
        val rawText = readRawFile(context).ifBlank { readGuidiFile(context) ?: "" }
        val journalPrompt = com.tatl.fastnote.data.user.LanguageManager.getGeminiJournalPrompt()
        if (rawText.isNotBlank()) {
            val lines = rawText.lines()
            val filteredLines = filterForAiSharing(lines)
            val fullContent = buildString {
                appendLine("[AI INSTRUCTION / YÊU CẦU CHO AI]: $journalPrompt")
                appendLine("==================================================")
                appendLine()
                append(filteredLines.joinToString("\n\n"))
            }
            cleanFile.writeText(fullContent, Charsets.UTF_8)
        }
        return cleanFile
    }
}
