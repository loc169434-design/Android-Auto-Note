package com.tatl.fastnote.ui.ai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.tatl.fastnote.R
import com.tatl.fastnote.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Transparent activity triggered from TripleActionWidget's Gemini button.
 *
 * Sends fileguidi.txt as a real .txt file attachment to Gemini via ACTION_SEND + EXTRA_STREAM.
 * If Gemini doesn't handle file attachments directly, falls back to system share sheet
 * (user picks Gemini manually).
 *
 * Flow:
 *  1. fileguidi.txt exists → share as .txt file via FileProvider
 *       a. Try targeting Gemini directly (setPackage)
 *       b. If rejected → open system share sheet (user selects Gemini)
 *  2. fileguidi.txt empty/missing → launch Gemini normally + toast
 *  3. Gemini not installed → Play Store
 */
class GeminiLaunchActivity : ComponentActivity() {

    companion object {
        private const val GEMINI_PACKAGE = "com.google.android.apps.bard"
        private const val GEMINI_PLAY_URL =
            "https://play.google.com/store/apps/details?id=com.google.android.apps.bard"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Overlay pulse animation — hiện trong khi IO đang chạy nền
        setContent {
            val pulse by rememberInfiniteTransition(label = "ai_pulse").animateFloat(
                initialValue = 0.85f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ai_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000)), // nền tối mờ nhẹ
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(pulse)
                        .background(Color(0xFF0D1B27).copy(alpha = 0.92f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ai),
                        contentDescription = "AI đang chuẩn bị",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        // Chạy IO nền, finish khi xong → overlay tự mất
        lifecycleScope.launch {
            openGemini()
            finish()
        }
    }

    private suspend fun openGemini() {
        val pm = packageManager
        val launchIntent = pm.getLaunchIntentForPackage(GEMINI_PACKAGE)

        if (launchIntent == null) {
            openPlayStore()
            return
        }

        val isBypass = com.tatl.fastnote.util.SecretDevModeManager.isBypassSecurityLayer1(this)

        // ⚡ Đọc file + lọc regex trên IO thread — tránh freeze Main Thread
        val aiSharedFile = withContext(Dispatchers.IO) {
            FileHelper.getAiSharedFile(this@GeminiLaunchActivity, bypassLayer1 = isBypass)
        }

        if (aiSharedFile.exists() && aiSharedFile.length() > 0) {
            // Build a content:// URI via FileProvider so Gemini can read the file
            val uri: Uri = try {
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    aiSharedFile
                )
            } catch (e: Exception) {
                // FileProvider misconfigured — fall back to text share
                shareAsText(launchIntent)
                return
            }

            val journalPrompt = com.tatl.fastnote.data.user.LanguageManager.getGeminiJournalPrompt()

            // 1. Sao chép câu lệnh Prompt vào khay nhớ tạm để người dùng có thể Dán trực tiếp vào ô chat Gemini nếu cần
            try {
                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                if (clipboard != null) {
                    val clip = android.content.ClipData.newPlainText("Gemini Prompt", journalPrompt)
                    clipboard.setPrimaryClip(clip)
                }
            } catch (e: Exception) {
                // Ignore clipboard error
            }

            // Build ACTION_SEND intent with the .txt file as attachment and localized journal prompt
            val fileIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, journalPrompt)
                putExtra(Intent.EXTRA_SUBJECT, journalPrompt)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Try sending directly to Gemini first
            val geminiDirect = Intent(fileIntent).apply {
                setPackage(GEMINI_PACKAGE)
            }

            val canHandleDirect = pm.resolveActivity(geminiDirect, 0) != null

            if (canHandleDirect) {
                startActivity(geminiDirect)
            } else {
                // Gemini can't handle file intent directly —
                // open system share sheet so user can pick Gemini
                val title = com.tatl.fastnote.data.user.LanguageManager.getSharedFileTitle()
                val chooser = Intent.createChooser(fileIntent, title)
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                startActivity(chooser)
            }
            return
        }

        // Chưa có ghi chú → mở Recording để ghi âm trước
        val noNotesMsg = when (com.tatl.fastnote.data.user.LanguageManager.currentLanguage.value) {
            com.tatl.fastnote.data.user.AppLanguage.VIETNAMESE -> "Chưa có ghi chú. Hãy ghi âm trước!"
            com.tatl.fastnote.data.user.AppLanguage.JAPANESE   -> "メモがありません。先に録音してください！"
            com.tatl.fastnote.data.user.AppLanguage.GERMAN     -> "Noch keine Notizen. Bitte zuerst aufnehmen!"
            com.tatl.fastnote.data.user.AppLanguage.RUSSIAN    -> "Нет заметок. Сначала запишите!"
            else -> "No notes yet. Please record first!"
        }

        // Mở Recording thay vì Gemini
        val recordIntent = Intent(this, com.tatl.fastnote.ui.recording.RecordingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(recordIntent)
    }

    /** Last-resort fallback: paste content as plain text into Gemini */
    private fun shareAsText(launchIntent: Intent) {
        val text = FileHelper.readGuidiFile(this)
        if (!text.isNullOrBlank()) {
            try {
                val journalPrompt = com.tatl.fastnote.data.user.LanguageManager.getGeminiJournalPrompt()
                val textIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "$journalPrompt\n\n${text.takeLast(30_000)}")
                    putExtra(Intent.EXTRA_SUBJECT, journalPrompt)
                    setPackage(GEMINI_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(textIntent)
                return
            } catch (_: Exception) {}
        }
        // Final fallback: just open Gemini
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launchIntent)
    }

    private fun openPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$GEMINI_PACKAGE")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(GEMINI_PLAY_URL)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (ex: Exception) {
            }
        }
    }
}
