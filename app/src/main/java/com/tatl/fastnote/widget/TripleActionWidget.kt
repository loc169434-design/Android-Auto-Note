package com.tatl.fastnote.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.tatl.fastnote.R
import com.tatl.fastnote.ui.ai.GeminiLaunchActivity
import com.tatl.fastnote.ui.fileviewer.FileViewerActivity
import com.tatl.fastnote.ui.recording.RecordingActivity

/**
 * Widget: Triple Action 1×3
 * Thiết kế OLED đen — container rounded xám tối, 3 SVG icon xám nhẹ.
 *
 * Layout:
 *   [  🎙  |  🧠  |  📓  ]
 *   ic_mic   ic_ai  ic_note
 *
 * Click:
 *   🎙 → RecordingActivity
 *   🧠 → GeminiLaunchActivity
 *   📓 → FileViewerActivity
 */
class TripleActionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        // Outer: toàn màn hình, nền trong suốt (launcher wallpaper hiện qua)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Inner container: rounded pill, nền xám đậm — đây là "hộp" trong ảnh
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(22.dp)
                    .background(
                        ColorProvider(
                            day = Color(0xFF000000),
                            night = Color(0xFF000000)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Mic ───────────────────────────────────────────────────────
                IconButton(
                    iconRes = R.drawable.ic_mic,
                    contentDescription = "Ghi âm",
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(actionStartActivity<RecordingActivity>())
                )

                Spacer(modifier = GlanceModifier.width(4.dp))

                // ── AI / Brain ────────────────────────────────────────────────
                IconButton(
                    iconRes = R.drawable.ic_ai,
                    contentDescription = "AI",
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(actionStartActivity<GeminiLaunchActivity>())
                )

                Spacer(modifier = GlanceModifier.width(4.dp))

                // ── Note / File ───────────────────────────────────────────────
                IconButton(
                    iconRes = R.drawable.ic_note,
                    contentDescription = "Ghi chú",
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(actionStartActivity<FileViewerActivity>())
                )
            }
        }
    }

    /**
     * Một ô icon — trong suốt, icon SVG căn giữa, không label.
     */
    @Composable
    private fun IconButton(
        iconRes: Int,
        contentDescription: String,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Box(
            modifier = modifier
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = contentDescription,
                modifier = GlanceModifier.size(30.dp)
            )
        }
    }
}

class TripleActionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TripleActionWidget()
}
