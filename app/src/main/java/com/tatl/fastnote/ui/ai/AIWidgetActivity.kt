package com.tatl.fastnote.ui.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.tatl.fastnote.AutoNoteApplication
import com.tatl.fastnote.MainActivity
import com.tatl.fastnote.billing.PremiumManager
import com.tatl.fastnote.billing.TrialManager
import com.tatl.fastnote.util.AIShareHelper
import kotlinx.coroutines.launch

/**
 * Transparent launcher activity triggered when clicking the AI Widget.
 * - From Day 31+ without Premium: Blocks AI and opens the Premium upgrade dialog.
 * - Otherwise: Gathers today's notes (privacy protected), launches AI share, and closes.
 */
class AIWidgetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as AutoNoteApplication

        lifecycleScope.launch {
            try {
                val isPrem = PremiumManager.isPremium(this@AIWidgetActivity)
                val isExpired = TrialManager.isTrialExpired(this@AIWidgetActivity)

                if (!isPrem && isExpired) {
                    val intent = Intent(this@AIWidgetActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("SHOW_PREMIUM_DIALOG", true)
                    }
                    startActivity(intent)
                    return@launch
                }

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
