package com.tatl.fastnote.ui.ai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import com.tatl.fastnote.util.FileHelper

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
        openGemini()
        finish()
    }

    private fun openGemini() {
        val pm = packageManager
        val launchIntent = pm.getLaunchIntentForPackage(GEMINI_PACKAGE)

        if (launchIntent == null) {
            openPlayStore()
            return
        }

        val isBypass = com.tatl.fastnote.util.SecretDevModeManager.isBypassSecurityLayer1(this)
        val aiSharedFile = FileHelper.getAiSharedFile(this, bypassLayer1 = isBypass)

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

        // No file yet
        val msg = "Chưa có ghi chú. Hãy ghi âm trước!"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        // Launch Gemini normally without attachment
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launchIntent)
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
                Toast.makeText(this, "Không thể mở Gemini", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
