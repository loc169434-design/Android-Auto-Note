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
    val promptText: String,
    val aiJournalPrompt: String
) {
    VIETNAMESE(
        code = "vi",
        shortCode = "VN",
        displayName = "Tiếng Việt",
        flagEmoji = "🇻🇳",
        speechTag = "vi-VN",
        promptText = "HÃY NÓI ĐIỀU BẠN MUỐN GHI CHÚ",
        aiJournalPrompt = "Đây là sổ nhật ký của tôi. Bạn hãy đọc, nắm rõ dữ liệu và chờ yêu cầu tiếp theo."
    ),
    ENGLISH(
        code = "en",
        shortCode = "EN",
        displayName = "English",
        flagEmoji = "🇬🇧",
        speechTag = "en-US",
        promptText = "PLEASE SAY WHAT YOU WANT TO NOTE",
        aiJournalPrompt = "This is my journal. Please read, understand the data, and wait for my next request."
    ),
    JAPANESE(
        code = "ja",
        shortCode = "JP",
        displayName = "日本語",
        flagEmoji = "🇯🇵",
        speechTag = "ja-JP",
        promptText = "メモしたい内容を話してください",
        aiJournalPrompt = "これは私の日記です。データをよく読み、理解した上で次の指示をお待ちください。"
    ),
    GERMAN(
        code = "de",
        shortCode = "DE",
        displayName = "Deutsch",
        flagEmoji = "🇩🇪",
        speechTag = "de-DE",
        promptText = "BITTE SPRECHEN SIE, WAS SIE NOTIEREN MÖCHTEN",
        aiJournalPrompt = "Dies ist mein Tagebuch. Bitte lesen und verstehen Sie die Daten und warten Sie auf meine nächste Anfrage."
    ),
    RUSSIAN(
        code = "ru",
        shortCode = "RU",
        displayName = "Русский",
        flagEmoji = "🇷🇺",
        speechTag = "ru-RU",
        promptText = "ПОЖАЛУЙСТA, СКАЖИТЕ, ЧТО ВЫ ХОТИТЕ ЗАПИСАТЬ",
        aiJournalPrompt = "Это мой дневник. Пожалуйста, прочитайте, усвойте данные и ожидайте следующего запроса."
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
        if (!prefs.contains(KEY_SELECTED_LANGUAGE)) {
            // Lần đầu vào app: Kiểm tra ngôn ngữ hệ thống của máy
            val systemLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.resources.configuration.locales.get(0) ?: Locale.getDefault()
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale ?: Locale.getDefault()
            }
            val sysLangCode = systemLocale.language.lowercase(Locale.ROOT)
            val matchedLang = AppLanguage.entries.find { it.code.equals(sysLangCode, ignoreCase = true) }
            val initialLang = matchedLang ?: AppLanguage.ENGLISH
            prefs.edit().putString(KEY_SELECTED_LANGUAGE, initialLang.code).apply()
            _currentLanguage.value = initialLang
            applyLocale(initialLang)
        } else {
            val savedCode = prefs.getString(KEY_SELECTED_LANGUAGE, AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
            val appLanguage = AppLanguage.fromCode(savedCode)
            _currentLanguage.value = appLanguage
            applyLocale(appLanguage)
        }
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
        try {
            val locale = Locale.forLanguageTag(language.code)
            Locale.setDefault(locale)
            val localeList = LocaleListCompat.forLanguageTags(language.code)
            AppCompatDelegate.setApplicationLocales(localeList)
        } catch (_: Exception) {}
    }

    fun getLocalizedContext(context: Context): Context {
        if (!::prefs.isInitialized) {
            init(context)
        }
        val lang = _currentLanguage.value
        val locale = Locale.forLanguageTag(lang.code)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(locale))
        }
        return context.createConfigurationContext(config)
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

    /**
     * Lấy prompt chỉ định cho Gemini khi chia sẻ file sổ nhật ký theo ngôn ngữ app.
     */
    fun getGeminiJournalPrompt(): String {
        return _currentLanguage.value.aiJournalPrompt
    }
}
