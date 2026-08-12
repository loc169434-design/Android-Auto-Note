package com.tatl.fastnote.ui.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.tatl.fastnote.AutoNoteApplication
import com.tatl.fastnote.util.AIShareHelper
import kotlinx.coroutines.launch

/**
 * Transparent launcher activity triggered when clicking the 1x1 AI Widget.
 * Automatically gathers today's notes as formatted text, launches AI share, and closes.
 */
class AIWidgetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as AutoNoteApplication

        lifecycleScope.launch {
            try {
                val todayText = app.noteRepository.exportTodayAsText()
                AIShareHelper.launchAIShare(this@AIWidgetActivity, todayText)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                finish()
            }
        }
    }
}
