package com.tatl.fastnote.widget

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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tatl.fastnote.ui.recording.RecordingActivity

/**
 * Widget 1: Quick Record — A simple button to start voice recording.
 */
class QuickRecordWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                QuickRecordContent()
            }
        }
    }

    @Composable
    private fun QuickRecordContent() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(ColorProvider(day = Color(0xFF1565C0), night = Color(0xFF0D47A1)))
                .clickable(actionStartActivity<RecordingActivity>())
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    provider = ImageProvider(android.R.drawable.ic_btn_speak_now),
                    contentDescription = "Ghi âm",
                    modifier = GlanceModifier.size(36.dp)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Ghi chú nhanh",
                    style = TextStyle(
                        color = ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

class QuickRecordWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickRecordWidget()
}
