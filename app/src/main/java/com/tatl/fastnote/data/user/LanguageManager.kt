package com.tatl.fastnote.data.user

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * 5 ngôn ngữ chuẩn theo Bản Đặc Tả V38 (Phần 3):
 *  - VN (vi-VN): "HÃY NÓI ĐIỀU BẠN MUỐN GHI CHÚ"
 *  - EN (en-US): "PLEASE SAY WHAT YOU WANT TO NOTE" (Mặc định Cold-Start)
 *  - JP (ja-JP): "メモしたい内容を話してください"
 *  - DE (de-DE): "BITTE SPRECHEN SIE, WAS SIE NOTIEREN MÖCHTEN"
 *  - RU (ru-RU): "ПОЖАЛУЙСТА, СКАЖИТЕ, ЧТО ВЫ ХОТИТЕ ЗАПИСАТЬ"
 */
enum class AppLanguage(
    val code: String,
    val shortCode: String,
    val displayName: String,
    val flagEmoji: String,
    val speechTag: String,
    val promptText: String
) {
    VIETNAMESE(
        code = "vi",
        shortCode = "VN",
        displayName = "Tiếng Việt",
        flagEmoji = "🇻🇳",
        speechTag = "vi-VN",
        promptText = "HÃY NÓI ĐIỀU BẠN MUỐN GHI CHÚ"
    ),
    ENGLISH(
        code = "en",
        shortCode = "EN",
        displayName = "English",
        flagEmoji = "🇬🇧",
        speechTag = "en-US",
        promptText = "PLEASE SAY WHAT YOU WANT TO NOTE"
    ),
    JAPANESE(
        code = "ja",
        shortCode = "JP",
        displayName = "日本語",
        flagEmoji = "🇯🇵",
        speechTag = "ja-JP",
        promptText = "メモしたい内容を話してください"
    ),
    GERMAN(
        code = "de",
        shortCode = "DE",
        displayName = "Deutsch",
        flagEmoji = "🇩🇪",
        speechTag = "de-DE",
        promptText = "BITTE SPRECHEN SIE, WAS SIE NOTIEREN MÖCHTEN"
    ),
    RUSSIAN(
        code = "ru",
        shortCode = "RU",
        displayName = "Русский",
        flagEmoji = "🇷🇺",
        speechTag = "ru-RU",
        promptText = "ПОЖАЛУЙСТА, СКАЖИТЕ, ЧТО ВЫ ХОТИТЕ ЗАПИСАТЬ"
    );

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }

        fun fromShortCode(shortCode: String): AppLanguage {
            return entries.find { it.shortCode.equals(shortCode, ignoreCase = true) } ?: ENGLISH
        }

        fun fromSpeechTag(speechTag: String): AppLanguage {
            return entries.find { it.speechTag.equals(speechTag, ignoreCase = true) } ?: ENGLISH
        }
    }
}

object LanguageManager {
    private const val PREFS_NAME = "auto_note_language_prefs"
    private const val KEY_SELECTED_LANGUAGE = "selected_language"

    private lateinit var prefs: SharedPreferences

    // Theo V38 Phần 3: Khi chưa thiết lập trong SharedPreferences -> Mặc định tiếng Anh
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_SELECTED_LANGUAGE, AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
        val appLanguage = AppLanguage.fromCode(savedCode)
        _currentLanguage.value = appLanguage
        applyLocale(appLanguage)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, language.code).apply()
        _currentLanguage.value = language
        applyLocale(language)
    }

    private fun applyLocale(language: AppLanguage) {
        val localeList = LocaleListCompat.forLanguageTags(language.code)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getSpeechLanguageTag(): String {
        return _currentLanguage.value.speechTag
    }

    fun getCurrentPromptText(): String {
        return _currentLanguage.value.promptText
    }

    /**
     * Tên tệp gửi AI theo quy chuẩn V38 Phần 6:
     * - Tiếng Việt: File_gui_di_(Da_loc_bao_mat).txt
     * - Quốc tế: Shared_File_(Privacy_Protected).txt
     */
    fun getSharedFileName(): String {
        return if (_currentLanguage.value == AppLanguage.VIETNAMESE) {
            "File_gui_di_(Da_loc_bao_mat).txt"
        } else {
            "Shared_File_(Privacy_Protected).txt"
        }
    }

    fun getSharedFileTitle(): String {
        return if (_currentLanguage.value == AppLanguage.VIETNAMESE) {
            "File gửi đi (Đã lọc bảo mật).txt"
        } else {
            "Shared File (Privacy Protected).txt"
        }
    }
}
