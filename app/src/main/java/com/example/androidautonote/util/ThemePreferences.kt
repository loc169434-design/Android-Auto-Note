package com.example.androidautonote.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages app theme preference using SharedPreferences.
 * Theme is auto-saved and restored on app start.
 */
object ThemePreferences {

    private const val PREFS_NAME = "auto_note_prefs"
    private const val KEY_THEME = "selected_theme"

    private lateinit var prefs: SharedPreferences

    private val _currentTheme = MutableStateFlow(AppTheme.OCEAN_BLUE)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTheme = prefs.getString(KEY_THEME, AppTheme.OCEAN_BLUE.name) ?: AppTheme.OCEAN_BLUE.name
        _currentTheme.value = try {
            AppTheme.valueOf(savedTheme)
        } catch (e: IllegalArgumentException) {
            AppTheme.OCEAN_BLUE
        }
    }

    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }
}

/**
 * Available app color themes
 */
enum class AppTheme(val displayName: String, val emoji: String) {
    OCEAN_BLUE("Đại dương", "🌊"),
    FOREST_GREEN("Rừng xanh", "🌲"),
    SUNSET_ORANGE("Hoàng hôn", "🌅"),
    LAVENDER("Lavender", "💜"),
    ROSE_PINK("Hồng", "🌸"),
    MIDNIGHT("Nửa đêm", "🌙"),
    COFFEE("Cà phê", "☕")
}
