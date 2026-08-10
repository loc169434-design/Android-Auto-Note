package com.example.androidautonote.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.androidautonote.ui.ai.GeminiLaunchActivity
import com.example.androidautonote.ui.fileviewer.FileViewerActivity
import com.example.androidautonote.ui.recording.RecordingActivity

/**
 * Widget: Triple Action (1×3)
 * Three buttons in a horizontal row:
 *  1. 🎙 Micro  → opens RecordingActivity (voice note)
 *  2. ✨ Gemini → opens Gemini app or Play Store via GeminiLaunchActivity
 *  3. 📂 File   → (to be implemented later)
 *
 * Supports a "highlight" blink state (triggered by WidgetPlacedReceiver)
 * to help the user spot the widget right after placing it.
 */
class TripleActionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read highlight state — set by WidgetPlacedReceiver when widget is placed
        val isHighlighted = WidgetPlacedReceiver.isHighlighted(context)

        provideContent {
            GlanceTheme {
                TripleActionContent(isHighlighted = isHighlighted)
            }
        }
    }

    @Composable
    private fun TripleActionContent(isHighlighted: Boolean) {
        // Outer container: transparent normally, bright glow when highlighted.
        val outerModifier = if (isHighlighted) {
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(ColorProvider(
                    day = Color(0xBBFFFFFF),   // white glow on light wallpaper
                    night = Color(0x99FFFFFF)   // softer on dark wallpaper
                ))
                .padding(horizontal = 3.dp, vertical = 3.dp)
        } else {
            GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 2.dp)
        }

        Row(
            modifier = outerModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ─── Button 1: Micro ──────────────────────────────
            ActionButton(
                iconRes = android.R.drawable.ic_btn_speak_now,
                label = "Micro",
                bgDayColor   = if (isHighlighted) Color(0xFF42A5F5) else Color(0xFF1565C0),
                bgNightColor = if (isHighlighted) Color(0xFF1E88E5) else Color(0xFF0D47A1),
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .clickable(actionStartActivity<RecordingActivity>())
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // ─── Button 2: Gemini ─────────────────────────────
            ActionButton(
                iconRes = android.R.drawable.btn_star_big_on,
                label = "Gemini",
                bgDayColor   = if (isHighlighted) Color(0xFFAB47BC) else Color(0xFF6A1B9A),
                bgNightColor = if (isHighlighted) Color(0xFF8E24AA) else Color(0xFF4A148C),
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .clickable(actionStartActivity<GeminiLaunchActivity>())
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // ─── Button 3: Open File ─────────────────────
            ActionButton(
                iconRes = android.R.drawable.ic_menu_agenda,
                label = "File",
                bgDayColor   = if (isHighlighted) Color(0xFF66BB6A) else Color(0xFF2E7D32),
                bgNightColor = if (isHighlighted) Color(0xFF43A047) else Color(0xFF1B5E20),
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .clickable(actionStartActivity<FileViewerActivity>())
            )
        }
    }

    @Composable
    private fun ActionButton(
        iconRes: Int,
        label: String,
        bgDayColor: Color,
        bgNightColor: Color,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Box(
            modifier = modifier
                .cornerRadius(14.dp)
                .background(ColorProvider(day = bgDayColor, night = bgNightColor))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = label,
                    modifier = GlanceModifier.size(28.dp)
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = label,
                    style = TextStyle(
                        color = ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

class TripleActionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TripleActionWidget()
}
