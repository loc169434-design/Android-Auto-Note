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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tatl.fastnote.AutoNoteApplication
import com.tatl.fastnote.MainActivity
import com.tatl.fastnote.util.DateUtils
import kotlinx.coroutines.flow.first

/**
 * Widget: Open Notes File — Shows a preview of today's note count
 * and opens MainActivity (all notes timeline) when clicked.
 */
class OpenNotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val noteCount = try {
            val app = context.applicationContext as AutoNoteApplication
            app.noteRepository.noteCount.first()
        } catch (e: Exception) {
            0
        }

        val todayCount = try {
            val app = context.applicationContext as AutoNoteApplication
            app.noteRepository.getNotesCountToday().first()
        } catch (e: Exception) {
            0
        }

        provideContent {
            GlanceTheme {
                OpenNotesContent(noteCount = noteCount, todayCount = todayCount)
            }
        }
    }

    @Composable
    private fun OpenNotesContent(noteCount: Int, todayCount: Int) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(ColorProvider(day = Color(0xFF2E7D32), night = Color(0xFF1B5E20)))
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // File icon
                Image(
                    provider = ImageProvider(android.R.drawable.ic_menu_agenda),
                    contentDescription = "Mở ghi chú",
                    modifier = GlanceModifier.size(32.dp)
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Title
                Text(
                    text = "📋 Mở ghi chú",
                    style = TextStyle(
                        color = ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Stats row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hôm nay: $todayCount",
                        style = TextStyle(
                            color = ColorProvider(
                                day = Color.White.copy(alpha = 0.85f),
                                night = Color.White.copy(alpha = 0.85f)
                            ),
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "Tổng: $noteCount",
                        style = TextStyle(
                            color = ColorProvider(
                                day = Color.White.copy(alpha = 0.7f),
                                night = Color.White.copy(alpha = 0.7f)
                            ),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

class OpenNotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OpenNotesWidget()
}
