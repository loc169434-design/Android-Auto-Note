package com.tatl.fastnote.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Quản lý tính năng ẩn (Easter Egg / Dev Mode):
 * Cho phép mở khóa bỏ qua Lớp bảo mật 1 (lọc số 9, 10, 12, 16 chữ số thành ***)
 * khi gửi dữ liệu ghi chú lên Gemini / AI.
 *
 * Thao tác mở: Chạm nhanh 7 lần liên tiếp vào dòng phiên bản nhỏ "v1.0.0 (X)" ở góc trên bên phải màn hình Sổ.
 */
object SecretDevModeManager {

    private const val PREFS_NAME = "secret_dev_prefs"
    private const val KEY_BYPASS_LAYER_1 = "bypass_security_layer_1"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Kiểm tra xem chế độ bỏ qua bảo mật số (Layer 1) có đang được kích hoạt hay không.
     */
    fun isBypassSecurityLayer1(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_BYPASS_LAYER_1, false)
    }

    /**
     * Bật / Tắt chế độ ẩn bỏ qua bảo mật số và lưu vào SharedPreferences.
     * Trả về trạng thái mới sau khi chuyển đổi.
     */
    fun toggleBypassSecurityLayer1(context: Context): Boolean {
        val current = isBypassSecurityLayer1(context)
        val newValue = !current
        prefs(context).edit().putBoolean(KEY_BYPASS_LAYER_1, newValue).apply()
        return newValue
    }
}
