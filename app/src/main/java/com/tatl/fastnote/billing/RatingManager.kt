package com.tatl.fastnote.billing

import android.content.Context

/**
 * Quản lý logic hiển thị dialog đánh giá ứng dụng (1 lần duy nhất, sau 72h kể từ khi nâng cấp Premium).
 */
object RatingManager {

    /** ⚠️ DEBUG ONLY — set true để luôn hiện dialog rating (bỏ qua 72h & shown-flag). Đặt false trước khi release. */
    private const val DEBUG_FORCE_RATING = false

    private const val PREFS       = "rating_prefs"
    private const val KEY_AT      = "premium_activated_at_ms"
    private const val KEY_SHOWN   = "rating_dialog_shown"

    /** 72 giờ tính bằng milliseconds */
    private const val DELAY_MS    = 72L * 60 * 60 * 1_000

    /**
     * Ghi nhận thời điểm nâng cấp Premium lần đầu tiên.
     * Chỉ ghi lần đầu — không ghi đè nếu đã có.
     */
    fun recordPremiumActivation(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_AT)) {
            prefs.edit().putLong(KEY_AT, System.currentTimeMillis()).apply()
        }
    }

    /**
     * Dành cho người dùng cũ đã Premium trước khi RatingManager tồn tại.
     * Nếu họ là Premium nhưng KEY_AT chưa được set → đặt KEY_AT = (bây giờ - 72h)
     * để dialog hiện ngay ở lần Lưu tiếp theo sau khi cập nhật app.
     *
     * Gọi ở MainActivity.onCreate() sau khi biết user đang Premium.
     */
    fun seedForExistingPremium(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_AT)) {
            // Đặt mốc 72h trước → điều kiện thời gian thoả mãn ngay
            prefs.edit().putLong(KEY_AT, System.currentTimeMillis() - DELAY_MS).apply()
        }
    }

    /**
     * Kiểm tra xem có nên hiển thị dialog đánh giá không.
     * Điều kiện:
     *  1. Chưa từng hiển thị (KEY_SHOWN == false)
     *  2. Đã ghi nhận thời điểm nâng cấp (KEY_AT > 0)
     *  3. Đã qua ít nhất 72 giờ kể từ khi nâng cấp
     */
    fun shouldShowRatingDialog(context: Context): Boolean {
        if (DEBUG_FORCE_RATING) return true          // ⚠️ DEBUG
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SHOWN, false)) return false
        val activatedAt = prefs.getLong(KEY_AT, 0L)
        if (activatedAt == 0L) return false
        return System.currentTimeMillis() - activatedAt >= DELAY_MS
    }

    /**
     * Đánh dấu dialog đã hiển thị — sẽ không bao giờ hiện lại.
     */
    fun markDialogShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOWN, true).apply()
    }
}
