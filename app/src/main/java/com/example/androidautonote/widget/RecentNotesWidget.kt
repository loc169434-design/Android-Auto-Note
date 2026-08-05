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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.androidautonote.AutoNoteApplication
import com.example.androidautonote.MainActivity
import com.example.androidautonote.data.db.NoteEntity
import com.example.androidautonote.ui.recording.RecordingActivity
import com.example.androidautonote.util.DateUtils
import kotlinx.coroutines.flow.first

/**
 * Widget: Recent Notes — Timeline style with date groups,
 * mic button in header, matching reference image layout.
 */
class RecentNotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val notes = try {
            val app = context.applicationContext as AutoNoteApplication
            app.noteRepository.getRecentNotes(5).first()
        } catch (e: Exception) {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                RecentNotesContent(notes = notes)
            }
        }
    }

    @Composable
    private fun RecentNotesContent(notes: List<NoteEntity>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(ColorProvider(day = Color.White, night = Color(0xFF1E1E1E)))
                .padding(14.dp)
        ) {
            // Header: icon + title + mic button
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(android.R.drawable.ic_menu_recent_history),
                    contentDescription = null,
                    modifier = GlanceModifier.size(20.dp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "Ghi chú gần đây",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(day = Color(0xFF212121), night = Color(0xFFEEEEEE))
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                // Mic button
                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .cornerRadius(16.dp)
                        .background(ColorProvider(day = Color(0xFF1565C0), night = Color(0xFF0D47A1)))
                        .clickable(actionStartActivity<RecordingActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(android.R.drawable.ic_btn_speak_now),
                        contentDescription = "Ghi âm",
                        modifier = GlanceModifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            if (notes.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có ghi chú",
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = ColorProvider(day = Color.Gray, night = Color.Gray)
                        )
                    )
                }
            } else {
                // Notes list with timeline dots and date groups
                var lastDateKey = ""
                notes.forEach { note ->
                    val dateKey = DateUtils.getDateKey(note.createdAt)

                    // Date group header (if different day)
                    if (dateKey != lastDateKey && lastDateKey.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Text(
                            text = DateUtils.formatRelativeDay(note.createdAt),
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(day = Color(0xFF1565C0), night = Color(0xFF90CAF9))
                            ),
                            modifier = GlanceModifier.padding(bottom = 4.dp)
                        )
                    }
                    lastDateKey = dateKey

                    // Note entry: time + dot + content card
                    NoteTimelineItem(note = note)
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
            }
        }
    }

    @Composable
    private fun NoteTimelineItem(note: NoteEntity) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top
        ) {
            // Time column
            Column(
                horizontalAlignment = Alignment.End,
                modifier = GlanceModifier.width(42.dp)
            ) {
                Text(
                    text = DateUtils.formatTimeOnly(note.createdAt),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(day = Color(0xFF1565C0), night = Color(0xFF90CAF9))
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Timeline dot
            Box(
                modifier = GlanceModifier
                    .size(8.dp)
                    .cornerRadius(4.dp)
                    .background(ColorProvider(day = Color(0xFF1565C0), night = Color(0xFF90CAF9)))
            ) {}

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Content card
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .cornerRadius(10.dp)
                    .background(ColorProvider(day = Color(0xFFF5F5F5), night = Color(0xFF2C2C2C)))
                    .padding(10.dp)
            ) {
                Text(
                    text = note.title,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(day = Color(0xFF212121), night = Color(0xFFEEEEEE))
                    ),
                    maxLines = 2
                )
            }
        }
    }
}

class RecentNotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecentNotesWidget()
}
