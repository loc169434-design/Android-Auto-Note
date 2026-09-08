package com.tatl.fastnote.ui.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.tatl.fastnote.AutoNoteApplication
import com.tatl.fastnote.util.AIShareHelper
import kotlinx.coroutines.launch

/**
 * Transparent launcher activity triggered when clicking the AI Widget.
 * - Share AI luôn cho phép, kể cả khi hết hạn trial (không cần Premium để share).
 * - Gathers today's notes (privacy protected), launches AI share, and closes.
 */
class AIWidgetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as AutoNoteApplication

        lifecycleScope.launch {
            try {
                // Share AI luôn cho phép dù hết hạn trial
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
