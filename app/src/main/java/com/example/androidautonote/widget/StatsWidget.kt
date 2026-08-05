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
import kotlinx.coroutines.flow.first

/**
 * Widget: Stats — "Thống kê nhanh" with today count and total.
 * Green/teal background with edit icon (matching reference image).
 */
class StatsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (totalCount, todayCount) = try {
            val app = context.applicationContext as AutoNoteApplication
            val total = app.noteRepository.noteCount.first()
            val today = app.noteRepository.getNotesCountToday().first()
            Pair(total, today)
        } catch (e: Exception) {
            Pair(0, 0)
        }

        provideContent {
            GlanceTheme {
                StatsContent(totalCount = totalCount, todayCount = todayCount)
            }
        }
    }

    @Composable
    private fun StatsContent(totalCount: Int, todayCount: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(ColorProvider(day = Color(0xFFE8F5E9), night = Color(0xFF1B3A1D)))
                .clickable(actionStartActivity<MainActivity>())
                .padding(14.dp)
        ) {
            // Header row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thống kê nhanh",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(day = Color(0xFF2E7D32), night = Color(0xFF81C784))
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Image(
                    provider = ImageProvider(android.R.drawable.ic_menu_edit),
                    contentDescription = "Mở",
                    modifier = GlanceModifier.size(18.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Today count
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hôm nay:",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(day = Color(0xFF424242), night = Color(0xFFE0E0E0))
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = "$todayCount",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(day = Color(0xFF2E7D32), night = Color(0xFF66BB6A))
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Total count
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tổng:",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(day = Color(0xFF424242), night = Color(0xFFE0E0E0))
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = "$totalCount",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(day = Color(0xFF1565C0), night = Color(0xFF90CAF9))
                    )
                )
            }
        }
    }
}

class StatsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatsWidget()
}
