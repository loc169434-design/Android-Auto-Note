package com.tatl.fastnote.data.user

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val flagEmoji: String,
    val speechTag: String
) {
    SYSTEM("system", "Default", "📱", ""),
    VIETNAMESE("vi", "Tiếng Việt", "🇻🇳", "vi-VN"),
    ENGLISH("en", "English", "🇬🇧", "en-US"),
    GERMAN("de", "Deutsch", "🇩🇪", "de-DE"),
    JAPANESE("ja", "日本語", "🇯🇵", "ja-JP");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
        }
    }
}

object LanguageManager {
    private const val PREFS_NAME = "auto_note_language_prefs"
    private const val KEY_SELECTED_LANGUAGE = "selected_language"

    private lateinit var prefs: SharedPreferences

    private val _currentLanguage = MutableStateFlow(AppLanguage.SYSTEM)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_SELECTED_LANGUAGE, AppLanguage.SYSTEM.code) ?: AppLanguage.SYSTEM.code
        val appLanguage = AppLanguage.fromCode(savedCode)
        _currentLanguage.value = appLanguage
        applyLocale(appLanguage)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, language.code).apply()
        _currentLanguage.value = language
        applyLocale(language)
    }

    private fun applyLocale(language: AppLanguage) {
        val localeList = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.code)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getSpeechLanguageTag(): String {
        val current = _currentLanguage.value
        if (current != AppLanguage.SYSTEM && current.speechTag.isNotEmpty()) {
            return current.speechTag
        }
        // Fallback to system default locale tag or vi-VN
        val systemLocale = Locale.getDefault().toLanguageTag()
        return if (systemLocale.startsWith("en", ignoreCase = true)) "en-US"
        else if (systemLocale.startsWith("de", ignoreCase = true)) "de-DE"
        else if (systemLocale.startsWith("ja", ignoreCase = true)) "ja-JP"
        else "vi-VN"
    }
}
