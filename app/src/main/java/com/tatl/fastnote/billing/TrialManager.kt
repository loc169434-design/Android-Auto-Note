package com.tatl.fastnote.billing

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Manages the 30-day free trial.
 *
 * Day 1-27  → full access, no messages
 * Day 28-30 → Toast countdown shown each time app opens
 * Day 31+   → isTrialExpired() = true → soft-lock activates
 *
 * First launch date is stored in SharedPreferences and never reset.
 */
object TrialManager {

    private const val PREFS_NAME  = "trial_prefs"
    private const val KEY_FIRST   = "first_launch_date"
    private const val TRIAL_DAYS  = 30L

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── First launch setup ────────────────────────────────────────────────────

    /**
     * Call once in MainActivity.onCreate (after auth gate).
     * Records today as first launch date if not already set.
     */
    fun initFirstLaunch(ctx: Context) {
        val p = prefs(ctx)
        if (!p.contains(KEY_FIRST)) {
            p.edit().putString(KEY_FIRST, LocalDate.now().toString()).apply()
        }
    }

    // ── Core queries ──────────────────────────────────────────────────────────

    fun getFirstLaunchDate(ctx: Context): LocalDate? {
        val raw = prefs(ctx).getString(KEY_FIRST, null) ?: return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    fun getDaysUsed(ctx: Context): Long {
        val first = getFirstLaunchDate(ctx) ?: return 0L
        return ChronoUnit.DAYS.between(first, LocalDate.now()).coerceAtLeast(0)
    }

    fun getDaysLeft(ctx: Context): Long =
        (TRIAL_DAYS - getDaysUsed(ctx)).coerceAtLeast(0)

    /** Returns true when 30-day trial has expired (day 31+). */
    fun isTrialExpired(ctx: Context): Boolean = getDaysUsed(ctx) >= TRIAL_DAYS

    /**
     * Returns true if we should show the countdown Toast today (day 28-30).
     * Also tracks whether the toast was shown today to avoid repeating.
     */
    fun shouldShowCountdown(ctx: Context): Boolean {
        val days = getDaysUsed(ctx)
        return days in 27 until TRIAL_DAYS   // day 28, 29, 30 (0-indexed: 27, 28, 29)
    }

    fun getCountdownMessage(ctx: Context): String {
        val left = getDaysLeft(ctx)
        return "Bạn còn $left ngày trải nghiệm miễn phí. Nâng cấp ngay để giữ nhịp!"
    }

    // ── For testing: force-set first launch date ──────────────────────────────

    fun debugSetDaysUsed(ctx: Context, daysAgo: Long) {
        val date = LocalDate.now().minusDays(daysAgo)
        prefs(ctx).edit().putString(KEY_FIRST, date.toString()).apply()
    }
}
