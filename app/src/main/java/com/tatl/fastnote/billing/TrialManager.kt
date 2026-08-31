package com.tatl.fastnote.billing

import android.content.Context
import android.content.SharedPreferences
import com.tatl.fastnote.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Manages the 30-day free trial and lock flow (V38 & Checkpoint 7).
 *
 * Day 1-28 (daysUsed: 0-27) → Full free access, no banner on Mic screen
 * Day 29    (daysUsed: 28)   → Banner on Mic screen: "Bạn còn 02 ngày để sử dụng miễn phí..."
 * Day 30    (daysUsed: 29)   → Banner on Mic screen: "Bạn còn 01 ngày để sử dụng miễn phí..."
 * Day 31+   (daysUsed: >=30) → Banner on Mic screen: "Hãy nâng cấp để tiếp tục sử dụng!" (pinned forever)
 *                             + Lock new note saving (Mic still listens, but does NOT save to Home/raw.txt/DB)
 *                             + Lock AI brain widget/button (opens Premium dialog)
 *                             + Edit screen kept as-is
 */
object TrialManager {

    private const val PREFS_NAME          = "trial_prefs"
    private const val KEY_FIRST           = "first_launch_date"
    private const val KEY_LAST_SEEN       = "last_seen_date"
    private const val KEY_TAMPER_DETECTED = "tamper_detected"
    const val TRIAL_DAYS                  = 30L

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── First launch setup ────────────────────────────────────────────────────

    /**
     * Call once in MainActivity.onCreate.
     * Records today as first launch date if not already set.
     */
    fun initFirstLaunch(ctx: Context) {
        val p = prefs(ctx)
        val today = LocalDate.now().toString()
        if (!p.contains(KEY_FIRST)) {
            p.edit()
                .putString(KEY_FIRST, today)
                .putString(KEY_LAST_SEEN, today)
                .apply()
        } else {
            // Cập nhật watermark ngày
            getEffectiveDate(ctx)
        }
    }

    // ── Core queries (Anti-Clock-Tampering Shield) ─────────────────────────────

    fun getFirstLaunchDate(ctx: Context): LocalDate? {
        val raw = prefs(ctx).getString(KEY_FIRST, null) ?: return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    /**
     * Thuật toán lá chắn chống lùi giờ thiết bị (Monotonic Watermark Date):
     * - Hệ thống luôn ghi nhận ngày lớn nhất từng thấy (`last_seen_date`).
     * - Nếu người dùng vào Cài đặt điện thoại chỉnh lùi ngày về quá khứ (now < last_seen hoặc now < first_launch),
     *   hệ thống tự động phát hiện gian lận và sử dụng `last_seen_date` làm mốc tính toán.
     * - Thời gian chỉ có thể TIẾN, KHÔNG THỂ LÙI.
     */
    fun getEffectiveDate(ctx: Context): LocalDate {
        val p = prefs(ctx)
        val now = LocalDate.now()
        val first = getFirstLaunchDate(ctx) ?: now
        val lastSeenRaw = p.getString(KEY_LAST_SEEN, null)
        val lastSeen = runCatching { LocalDate.parse(lastSeenRaw) }.getOrNull() ?: first

        return when {
            // Trường hợp 1: Chỉnh lùi trước cả ngày cài app -> Giữ mốc lớn nhất
            now.isBefore(first) -> {
                p.edit().putBoolean(KEY_TAMPER_DETECTED, true).apply()
                lastSeen
            }
            // Trường hợp 2: Chỉnh lùi so với ngày mở app gần nhất -> Giữ mốc lớn nhất
            now.isBefore(lastSeen) -> {
                p.edit().putBoolean(KEY_TAMPER_DETECTED, true).apply()
                lastSeen
            }
            // Trường hợp bình thường: Cập nhật watermark tiến lên
            else -> {
                p.edit().putString(KEY_LAST_SEEN, now.toString()).apply()
                now
            }
        }
    }

    /**
     * Cập nhật thời gian thực từ Server (Google Drive / NTP / HTTP Date Header) khi có mạng.
     */
    fun updateServerDate(ctx: Context, serverDate: LocalDate) {
        val p = prefs(ctx)
        val lastSeenRaw = p.getString(KEY_LAST_SEEN, null)
        val lastSeen = runCatching { LocalDate.parse(lastSeenRaw) }.getOrNull()
        if (lastSeen == null || serverDate.isAfter(lastSeen)) {
            p.edit().putString(KEY_LAST_SEEN, serverDate.toString()).apply()
        }
    }

    fun getDaysUsed(ctx: Context): Long {
        val first = getFirstLaunchDate(ctx) ?: return 0L
        val effectiveDate = getEffectiveDate(ctx)
        return ChronoUnit.DAYS.between(first, effectiveDate).coerceAtLeast(0)
    }

    fun getDaysLeft(ctx: Context): Long =
        (TRIAL_DAYS - getDaysUsed(ctx)).coerceAtLeast(0)

    /** Returns true when 30-day trial has expired (day 31+). */
    fun isTrialExpired(ctx: Context): Boolean = getDaysUsed(ctx) >= TRIAL_DAYS

    /**
     * Kiểm tra xem màn hình Mic có cần hiển thị banner đếm ngược / nâng cấp không (từ ngày 29 trở đi).
     */
    fun shouldShowMicBanner(ctx: Context, isPremium: Boolean): Boolean {
        if (isPremium) return false
        val daysUsed = getDaysUsed(ctx)
        return daysUsed >= 28L // Day 29 (28 days elapsed), Day 30 (29), Day 31+ (>= 30)
    }

    /**
     * Lấy dòng text thông báo đếm ngược / nâng cấp theo ngôn ngữ app cho màn hình Mic:
     * - Ngày 29: "Bạn còn 02 ngày để sử dụng miễn phí. Hãy nâng cấp để sử dụng trọn đời!"
     * - Ngày 30: "Bạn còn 01 ngày để sử dụng miễn phí. Hãy nâng cấp để sử dụng trọn đời!"
     * - Ngày 31+: "Hãy nâng cấp để tiếp tục sử dụng!" (ghim cố định)
     */
    fun getMicBannerMessage(ctx: Context): String? {
        val daysUsed = getDaysUsed(ctx)
        return when {
            daysUsed == 28L -> ctx.getString(R.string.str_trial_countdown_2_days)
            daysUsed == 29L -> ctx.getString(R.string.str_trial_countdown_1_day)
            daysUsed >= 30L -> ctx.getString(R.string.str_trial_expired_permanent)
            else            -> null
        }
    }

    fun getSaveBlockedMessage(ctx: Context): String {
        return ctx.getString(R.string.str_trial_expired_save_blocked)
    }

    // ── Toast đếm ngược ngày 28-30 cho MainActivity nếu cần ─────────────────
    fun shouldShowCountdown(ctx: Context): Boolean {
        val days = getDaysUsed(ctx)
        return days in 27 until TRIAL_DAYS
    }

    fun getCountdownMessage(ctx: Context): String {
        val left = getDaysLeft(ctx)
        return ctx.getString(R.string.str_trial_countdown_1_day)
    }

    // ── For testing: force-set first launch date ──────────────────────────────
    fun debugSetDaysUsed(ctx: Context, daysAgo: Long) {
        val date = LocalDate.now().minusDays(daysAgo)
        prefs(ctx).edit().putString(KEY_FIRST, date.toString()).apply()
    }
}
