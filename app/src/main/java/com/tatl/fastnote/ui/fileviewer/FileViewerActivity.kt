package com.tatl.fastnote.ui.fileviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tatl.fastnote.ui.theme.AndroidAutoNoteTheme

/**
 * Full-screen note file viewer opened from the widget's File button
 * or from the HomeScreen Edit button.
 *
 * Intent extras:
 *   EXTRA_START_EDIT (Boolean) — open directly in edit mode
 */
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import java.util.Locale
import com.tatl.fastnote.data.user.LanguageManager

class FileViewerActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.getLocalizedContext(newBase))
    }

    companion object {
        const val EXTRA_START_EDIT = "start_edit"
        /** true khi mo tu ben trong app (HomeScreen), false khi mo tu widget */
        const val EXTRA_FROM_APP   = "from_app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startEdit = intent.getBooleanExtra(EXTRA_START_EDIT, false)
        val fromApp   = intent.getBooleanExtra(EXTRA_FROM_APP, false)

        setContent {
            AndroidAutoNoteTheme {
                val currentLanguage by LanguageManager.currentLanguage.collectAsState()
                val baseCtx = LocalContext.current
                val localizedContext = remember(currentLanguage) {
                    val config = Configuration(baseCtx.resources.configuration)
                    config.setLocale(Locale.forLanguageTag(currentLanguage.code))
                    baseCtx.createConfigurationContext(config)
                }
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalConfiguration provides localizedContext.resources.configuration
                ) {
                    FileViewerScreen(
                        startInEditMode = startEdit,
                        onClose = {
                            if (fromApp) {
                                // Mo tu app: finish() -> ve HomeScreen
                                finish()
                            } else {
                                // Mo tu widget: finishAndRemoveTask() -> thoat han app
                                finishAndRemoveTask()
                            }
                        }
                    )
                }
            }
        }
    }
}
