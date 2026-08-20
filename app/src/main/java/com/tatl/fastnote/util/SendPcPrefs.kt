package com.tatl.fastnote.util

import android.content.Context

/**
 * Luu mat khau "Gui PC" sau lan nhap dau tien.
 * Lan tiep theo bam nut -> lay mat khau nay tu SharedPrefs, khong hoi lai.
 */
object SendPcPrefs {

    private const val PREFS_NAME = "send_pc_prefs"
    private const val KEY_PASSWORD = "saved_password"

    /** Lay mat khau da luu. Null neu chua tung nhap. */
    fun getSavedPassword(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PASSWORD, null)
            ?.takeIf { it.isNotBlank() }
    }

    /** Luu mat khau sau lan nhap dau tien. */
    fun savePassword(context: Context, password: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    /** Xoa mat khau (dung khi can reset). */
    fun clearPassword(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PASSWORD)
            .apply()
    }
}
