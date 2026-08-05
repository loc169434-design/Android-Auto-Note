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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.androidautonote.AutoNoteApplication
import com.example.androidautonote.MainActivity
import kotlinx.coroutines.flow.first

/**
 * Widget: Open Notes — Shows total count with file icon.
 * Compact blue square (like the image: icon + number + "Ghi chú" label)
 */
class OpenNotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val noteCount = try {
            val app = context.applicationContext as AutoNoteApplication
            app.noteRepository.noteCount.first()
        } catch (e: Exception) {
            0
        }

        provideContent {
            GlanceTheme {
                OpenNotesContent(noteCount = noteCount)
            }
        }
    }

    @Composable
    private fun OpenNotesContent(noteCount: Int) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(ColorProvider(day = Color(0xFF1565C0), night = Color(0xFF0D47A1)))
                .clickable(actionStartActivity<MainActivity>())
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    provider = ImageProvider(android.R.drawable.ic_menu_agenda),
                    contentDescription = "Ghi chú",
                    modifier = GlanceModifier.size(28.dp)
                )
                Text(
                    text = "$noteCount",
                    style = TextStyle(
                        color = ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Ghi chú",
                    style = TextStyle(
                        color = ColorProvider(
                            day = Color.White.copy(alpha = 0.8f),
                            night = Color.White.copy(alpha = 0.8f)
                        ),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

class OpenNotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OpenNotesWidget()
}
