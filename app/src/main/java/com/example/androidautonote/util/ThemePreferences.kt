package com.example.androidautonote.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages all app preferences using SharedPreferences.
 * Theme, recording settings, etc.
 */
object ThemePreferences {

    private const val PREFS_NAME = "auto_note_prefs"
    private const val KEY_THEME = "selected_theme"
    private const val KEY_AUTO_STOP_SECONDS = "auto_stop_seconds"
    private const val KEY_RECOGNITION_LANGUAGE = "recognition_language"
    private const val KEY_VIBRATE_ON_RECORD = "vibrate_on_record"
    private const val KEY_CLOUD_SYNC = "cloud_sync"

    private lateinit var prefs: SharedPreferences

    // Theme
    private val _currentTheme = MutableStateFlow(AppTheme.OCEAN_BLUE)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    // Auto-stop duration (seconds)
    private val _autoStopSeconds = MutableStateFlow(30)
    val autoStopSeconds: StateFlow<Int> = _autoStopSeconds.asStateFlow()

    // Recognition language
    private val _recognitionLanguage = MutableStateFlow(RecognitionLang.VI)
    val recognitionLanguage: StateFlow<RecognitionLang> = _recognitionLanguage.asStateFlow()

    // Vibrate on start/stop
    private val _vibrateOnRecord = MutableStateFlow(true)
    val vibrateOnRecord: StateFlow<Boolean> = _vibrateOnRecord.asStateFlow()

    // Cloud sync (UI only)
    private val _cloudSync = MutableStateFlow(false)
    val cloudSync: StateFlow<Boolean> = _cloudSync.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedTheme = prefs.getString(KEY_THEME, AppTheme.OCEAN_BLUE.name) ?: AppTheme.OCEAN_BLUE.name
        _currentTheme.value = try {
            AppTheme.valueOf(savedTheme)
        } catch (e: IllegalArgumentException) {
            AppTheme.OCEAN_BLUE
        }

        _autoStopSeconds.value = prefs.getInt(KEY_AUTO_STOP_SECONDS, 30)

        val savedLang = prefs.getString(KEY_RECOGNITION_LANGUAGE, RecognitionLang.VI.name) ?: RecognitionLang.VI.name
        _recognitionLanguage.value = try {
            RecognitionLang.valueOf(savedLang)
        } catch (e: IllegalArgumentException) {
            RecognitionLang.VI
        }

        _vibrateOnRecord.value = prefs.getBoolean(KEY_VIBRATE_ON_RECORD, true)
        _cloudSync.value = prefs.getBoolean(KEY_CLOUD_SYNC, false)
    }

    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun setAutoStopSeconds(seconds: Int) {
        _autoStopSeconds.value = seconds
        prefs.edit().putInt(KEY_AUTO_STOP_SECONDS, seconds).apply()
    }

    fun setRecognitionLanguage(lang: RecognitionLang) {
        _recognitionLanguage.value = lang
        prefs.edit().putString(KEY_RECOGNITION_LANGUAGE, lang.name).apply()
    }

    fun setVibrateOnRecord(enabled: Boolean) {
        _vibrateOnRecord.value = enabled
        prefs.edit().putBoolean(KEY_VIBRATE_ON_RECORD, enabled).apply()
    }

    fun setCloudSync(enabled: Boolean) {
        _cloudSync.value = enabled
        prefs.edit().putBoolean(KEY_CLOUD_SYNC, enabled).apply()
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
