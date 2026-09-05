package com.tatl.fastnote.util

import com.tatl.fastnote.data.user.AppLanguage
import com.tatl.fastnote.data.user.LanguageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    // ── Localized day-of-week names ──────────────────────────────────────────
    private val dayOfWeekMap = mapOf(
        AppLanguage.VIETNAMESE to arrayOf(
            "", "Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"
        ),
        AppLanguage.ENGLISH to arrayOf(
            "", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        ),
        AppLanguage.JAPANESE to arrayOf(
            "", "日曜日", "月曜日", "火曜日", "水曜日", "木曜日", "金曜日", "土曜日"
        ),
        AppLanguage.GERMAN to arrayOf(
            "", "Sonntag", "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag"
        ),
        AppLanguage.RUSSIAN to arrayOf(
            "", "Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"
        )
    )

    // ── Localized "Today" / "Yesterday" ─────────────────────────────────────
    private val todayLabel = mapOf(
        AppLanguage.VIETNAMESE to "Hôm nay",
        AppLanguage.ENGLISH    to "Today",
        AppLanguage.JAPANESE   to "今日",
        AppLanguage.GERMAN     to "Heute",
        AppLanguage.RUSSIAN    to "Сегодня"
    )
    private val yesterdayLabel = mapOf(
        AppLanguage.VIETNAMESE to "Hôm qua",
        AppLanguage.ENGLISH    to "Yesterday",
        AppLanguage.JAPANESE   to "昨日",
        AppLanguage.GERMAN     to "Gestern",
        AppLanguage.RUSSIAN    to "Вчера"
    )

    // ── Localized date format patterns ──────────────────────────────────────
    // VI: 12/08/2026  |  EN: 08/12/2026  |  JP: 2026/08/12  |  DE/RU: 12.08.2026
    private val datePatternMap = mapOf(
        AppLanguage.VIETNAMESE to "dd/MM/yyyy",
        AppLanguage.ENGLISH    to "MM/dd/yyyy",
        AppLanguage.JAPANESE   to "yyyy/MM/dd",
        AppLanguage.GERMAN     to "dd.MM.yyyy",
        AppLanguage.RUSSIAN    to "dd.MM.yyyy"
    )

    private val dateTimePatternMap = mapOf(
        AppLanguage.VIETNAMESE to "dd/MM/yyyy HH:mm",
        AppLanguage.ENGLISH    to "MM/dd/yyyy h:mm a",
        AppLanguage.JAPANESE   to "yyyy/MM/dd HH:mm",
        AppLanguage.GERMAN     to "dd.MM.yyyy HH:mm",
        AppLanguage.RUSSIAN    to "dd.MM.yyyy HH:mm"
    )

    // EN dùng 12h AM/PM, các nước khác dùng 24h
    private val timePatternMap = mapOf(
        AppLanguage.VIETNAMESE to "HH:mm",
        AppLanguage.ENGLISH    to "h:mm a",
        AppLanguage.JAPANESE   to "HH:mm",
        AppLanguage.GERMAN     to "HH:mm",
        AppLanguage.RUSSIAN    to "HH:mm"
    )

    // ── Helpers: luôn lấy locale và pattern theo ngôn ngữ hiện tại ────────
    private fun currentLang(): AppLanguage = LanguageManager.currentLanguage.value

    private fun currentLocale(): Locale = Locale.forLanguageTag(currentLang().code)

    private fun dateFormat(): SimpleDateFormat =
        SimpleDateFormat(datePatternMap[currentLang()] ?: "dd/MM/yyyy", currentLocale())

    private fun dateTimeFormat(): SimpleDateFormat =
        SimpleDateFormat(dateTimePatternMap[currentLang()] ?: "dd/MM/yyyy HH:mm", currentLocale())

    private fun timeFormat(): SimpleDateFormat =
        SimpleDateFormat(timePatternMap[currentLang()] ?: "HH:mm", currentLocale())

    // ── Public API ──────────────────────────────────────────────────────────

    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat().format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat().format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat().format(Date(timestamp))
    }

    fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(currentLocale(), "%02d:%02d", mins, secs)
    }

    /**
     * Format time only — e.g. "08:00", "14:30"
     */
    fun formatTimeOnly(timestamp: Long): String {
        return timeFormat().format(Date(timestamp))
    }

    /**
     * Format day header for timeline — e.g.
     *   VI: "Thứ Hai, 10/08/2026"
     *   EN: "Monday, 08/10/2026"
     *   JP: "月曜日, 2026/08/10"
     *   DE: "Montag, 10.08.2026"
     *   RU: "Понедельник, 10.08.2026"
     */
    fun formatDayHeader(timestamp: Long): String {
        val lang = currentLang()
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val names = dayOfWeekMap[lang] ?: dayOfWeekMap[AppLanguage.ENGLISH]!!
        val dayOfWeek = names[cal.get(Calendar.DAY_OF_WEEK)]
        val dateStr = dateFormat().format(Date(timestamp))
        return "$dayOfWeek, $dateStr"
    }

    /**
     * Format relative day — "Hôm nay" / "Today" / "今日" / "Heute" / "Сегодня",
     *                        "Hôm qua" / "Yesterday" / etc., or full day header
     */
    fun formatRelativeDay(timestamp: Long): String {
        val lang = currentLang()
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            isSameDay(today, target) -> todayLabel[lang] ?: "Today"
            isYesterday(today, target) -> yesterdayLabel[lang] ?: "Yesterday"
            else -> formatDayHeader(timestamp)
        }
    }

    /**
     * Get a date key string for grouping — "yyyy-MM-dd" (internal, never displayed)
     */
    fun getDateKey(timestamp: Long): String {
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        return keyFormat.format(Date(timestamp))
    }

    /**
     * Check if two Calendar instances represent the same day
     */
    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(today: Calendar, target: Calendar): Boolean {
        val yesterday = today.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(yesterday, target)
    }
}
