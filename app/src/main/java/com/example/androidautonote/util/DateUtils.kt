package com.example.androidautonote.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Vietnamese day-of-week names
    private val dayOfWeekNames = arrayOf(
        "", "Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"
    )

    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    /**
     * Format time only — e.g. "08:00", "14:30"
     */
    fun formatTimeOnly(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    /**
     * Format day header for timeline — e.g. "Thứ Hai, 10/08/2026"
     */
    fun formatDayHeader(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val dayOfWeek = dayOfWeekNames[cal.get(Calendar.DAY_OF_WEEK)]
        val dateStr = dateFormat.format(Date(timestamp))
        return "$dayOfWeek, $dateStr"
    }

    /**
     * Format relative day — "Hôm nay", "Hôm qua", or full day header
     */
    fun formatRelativeDay(timestamp: Long): String {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            isSameDay(today, target) -> "Hôm nay"
            isYesterday(today, target) -> "Hôm qua"
            else -> formatDayHeader(timestamp)
        }
    }

    /**
     * Get a date key string for grouping — "yyyy-MM-dd"
     */
    fun getDateKey(timestamp: Long): String {
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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
