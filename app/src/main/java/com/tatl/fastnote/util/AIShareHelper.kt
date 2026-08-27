package com.tatl.fastnote.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Helper to detect installed AI apps and create share intents
 * for sending today's notes with a summarization prompt.
 */
object AIShareHelper {

    /**
     * Known AI app package names
     */
    private val AI_APPS = listOf(
        AIApp("Google Gemini", "com.google.android.apps.googleassistant"),
        AIApp("Google Gemini", "com.google.android.apps.bard"),
        AIApp("ChatGPT", "com.openai.chatgpt"),
        AIApp("Microsoft Copilot", "com.microsoft.copilot"),
        AIApp("Claude", "com.anthropic.claude")
    )

    data class AIApp(
        val displayName: String,
        val packageName: String
    )

    /**
     * Check which AI apps are installed on the device
     */
    fun getInstalledAIApps(context: Context): List<AIApp> {
        val pm = context.packageManager
        return AI_APPS.filter { app ->
            try {
                pm.getPackageInfo(app.packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * Create a share intent with today's notes and a summarization prompt.
     * If a specific AI app is found, target it directly.
     * Otherwise, open the general share chooser.
     */
    fun createAIShareIntent(
        context: Context,
        todayNotesText: String
    ): Intent {
        val journalPrompt = com.tatl.fastnote.data.user.LanguageManager.getGeminiJournalPrompt()
        val prompt = buildString {
            appendLine(journalPrompt)
            appendLine()
            append(todayNotesText)
        }

        val installedApps = getInstalledAIApps(context)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, prompt)
            putExtra(Intent.EXTRA_SUBJECT, journalPrompt)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // If an AI app is found, target it directly
        if (installedApps.isNotEmpty()) {
            shareIntent.setPackage(installedApps.first().packageName)
        }

        return shareIntent
    }

    /**
     * Launch AI share — tries direct AI app first, falls back to chooser
     */
    fun launchAIShare(
        context: Context,
        todayNotesText: String
    ) {
        if (todayNotesText.isBlank()) {
            val msg = if (com.tatl.fastnote.data.user.LanguageManager.currentLanguage.value == com.tatl.fastnote.data.user.AppLanguage.VIETNAMESE) {
                "Chưa có ghi chú nào. Hãy ghi âm trước!"
            } else {
                "No notes yet. Please record first!"
            }
            android.widget.Toast.makeText(
                context,
                msg,
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        val installedApps = getInstalledAIApps(context)
        val journalPrompt = com.tatl.fastnote.data.user.LanguageManager.getGeminiJournalPrompt()
        val promptTitle = com.tatl.fastnote.data.user.LanguageManager.getSharedFileTitle()

        try {
            if (installedApps.isNotEmpty()) {
                // Try direct AI app first
                val directIntent = createAIShareIntent(context, todayNotesText)
                context.startActivity(directIntent)
            } else {
                // No AI app found — open general chooser
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, buildString {
                        appendLine(journalPrompt)
                        appendLine()
                        append(todayNotesText)
                    })
                    putExtra(Intent.EXTRA_SUBJECT, journalPrompt)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(genericIntent, promptTitle)
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            // If direct app launch fails, fall back to chooser
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, buildString {
                        appendLine(journalPrompt)
                        appendLine()
                        append(todayNotesText)
                    })
                    putExtra(Intent.EXTRA_SUBJECT, journalPrompt)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(fallbackIntent, promptTitle)
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e2: Exception) {
                android.widget.Toast.makeText(
                    context,
                    "Không tìm thấy ứng dụng AI. Hãy cài Gemini hoặc ChatGPT.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
