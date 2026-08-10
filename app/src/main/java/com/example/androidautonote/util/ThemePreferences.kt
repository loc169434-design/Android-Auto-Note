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

    private const val KEY_WIDGET_PINNED = "has_pinned_widget"
    // Bump this version whenever widget layout changes require re-pinning
    private const val WIDGET_VERSION = 2
    private const val KEY_WIDGET_VERSION = "widget_version"

    private lateinit var prefs: SharedPreferences

    private val _currentTheme = MutableStateFlow(AppTheme.OCEAN_BLUE)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _hasPinnedWidget = MutableStateFlow(false)
    val hasPinnedWidget: StateFlow<Boolean> = _hasPinnedWidget.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTheme = prefs.getString(KEY_THEME, AppTheme.OCEAN_BLUE.name) ?: AppTheme.OCEAN_BLUE.name
        _currentTheme.value = try {
            AppTheme.valueOf(savedTheme)
        } catch (e: IllegalArgumentException) {
            AppTheme.OCEAN_BLUE
        }
        _hasPinnedWidget.value = prefs.getBoolean(KEY_WIDGET_PINNED, false)

        // Migration: if widget version changed, reset pin status so users
        // are prompted to add the new TripleActionWidget (replacing old widgets).
        val savedWidgetVersion = prefs.getInt(KEY_WIDGET_VERSION, 0)
        if (savedWidgetVersion < WIDGET_VERSION) {
            _hasPinnedWidget.value = false
            prefs.edit()
                .putBoolean(KEY_WIDGET_PINNED, false)
                .putInt(KEY_WIDGET_VERSION, WIDGET_VERSION)
                .apply()
        }
    }

    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    private const val KEY_RECOGNITION_LANG = "recognition_lang"

    private val _recognitionLanguage = MutableStateFlow(RecognitionLang.VI)
    val recognitionLanguage: StateFlow<RecognitionLang> = _recognitionLanguage.asStateFlow()

    fun setRecognitionLanguage(lang: RecognitionLang) {
        _recognitionLanguage.value = lang
        prefs.edit().putString(KEY_RECOGNITION_LANG, lang.name).apply()
    }

    fun setWidgetPinned(pinned: Boolean) {
        _hasPinnedWidget.value = pinned
        prefs.edit().putBoolean(KEY_WIDGET_PINNED, pinned).apply()
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

/**
 * Supported recognition languages
 */
enum class RecognitionLang(val displayName: String, val locale: String) {
    VI("Tiếng Việt (Mặc Định)", "vi-VN"),
    EN("English", "en-US"),
    JA("日本語", "ja-JP"),
    KO("한국어", "ko-KR"),
    ZH("中文", "zh-CN")
}
