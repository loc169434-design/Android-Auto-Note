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
class FileViewerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_EDIT = "start_edit"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startEdit = intent.getBooleanExtra(EXTRA_START_EDIT, false)

        setContent {
            AndroidAutoNoteTheme {
                FileViewerScreen(
                    startInEditMode = startEdit,
                    onClose = { finish() }
                )
            }
        }
    }
}
