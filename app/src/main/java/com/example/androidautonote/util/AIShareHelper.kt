package com.example.androidautonote.util

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
        val prompt = buildString {
            appendLine("Hãy tóm tắt những ghi chú trong ngày hôm nay của tôi một cách ngắn gọn và rõ ràng:")
            appendLine()
            append(todayNotesText)
        }

        val installedApps = getInstalledAIApps(context)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, prompt)
            putExtra(Intent.EXTRA_SUBJECT, "Tóm tắt ghi chú hôm nay")
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
            android.widget.Toast.makeText(
                context,
                "Chưa có ghi chú nào hôm nay để tóm tắt",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        val installedApps = getInstalledAIApps(context)

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
                        appendLine("Hãy tóm tắt những ghi chú trong ngày hôm nay của tôi một cách ngắn gọn và rõ ràng:")
                        appendLine()
                        append(todayNotesText)
                    })
                    putExtra(Intent.EXTRA_SUBJECT, "Tóm tắt ghi chú hôm nay")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(genericIntent, "Gửi tới AI để tóm tắt")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            // If direct app launch fails, fall back to chooser
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, buildString {
                        appendLine("Hãy tóm tắt những ghi chú trong ngày hôm nay của tôi:")
                        appendLine()
                        append(todayNotesText)
                    })
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(fallbackIntent, "Gửi tới AI để tóm tắt")
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
