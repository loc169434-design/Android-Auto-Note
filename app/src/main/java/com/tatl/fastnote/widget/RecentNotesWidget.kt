package com.tatl.fastnote.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tatl.fastnote.AutoNoteApplication
import com.tatl.fastnote.MainActivity
import com.tatl.fastnote.data.db.NoteEntity
import com.tatl.fastnote.ui.recording.RecordingActivity
import com.tatl.fastnote.util.DateUtils
import kotlinx.coroutines.flow.first

/**
 * Widget 2: Recent Notes — Shows up to 5 most recent notes.
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
                .cornerRadius(16.dp)
                .background(ColorProvider(day = Color(0xFFF1F5F9), night = Color(0xFF142433)))
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 Ghi chú gần đây",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(day = Color(0xFF1565C0), night = Color(0xFF90CAF9))
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(20.dp)
                        .background(ColorProvider(day = Color(0xFF1565C0), night = Color(0xFF0D47A1)))
                        .clickable(actionStartActivity<RecordingActivity>())
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+ Ghi",
                        style = TextStyle(
                            color = ColorProvider(day = Color.White, night = Color.White),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

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
                notes.forEach { note ->
                    NoteItem(note = note)
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
            }
        }
    }

    @Composable
    private fun NoteItem(note: NoteEntity) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(8.dp)
                .background(ColorProvider(day = Color(0xFFF5F5F5), night = Color(0xFF2C2C2C)))
                .clickable(actionStartActivity<MainActivity>())
                .padding(8.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = note.title,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorProvider(day = Color.Black, night = Color.White)
                        ),
                        maxLines = 1
                    )
                }
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = DateUtils.formatTime(note.createdAt),
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(day = Color.Gray, night = Color.Gray)
                    )
                )
            }
        }
    }
}

class RecentNotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecentNotesWidget()
}
