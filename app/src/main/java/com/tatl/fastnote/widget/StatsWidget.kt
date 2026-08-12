package com.tatl.fastnote.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tatl.fastnote.AutoNoteApplication
import kotlinx.coroutines.flow.first

/**
 * Widget 3: Stats — Shows total notes count and today's count.
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
                .cornerRadius(16.dp)
                .background(ColorProvider(day = Color.White, night = Color(0xFF1E1E1E)))
                .padding(16.dp)
        ) {
            Text(
                text = "📊 Thống kê",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(day = Color(0xFF1565C0), night = Color(0xFF90CAF9))
                )
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Total notes
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$totalCount",
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(day = Color(0xFF1565C0), night = Color(0xFF90CAF9))
                        )
                    )
                    Text(
                        text = "Tổng ghi chú",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = ColorProvider(day = Color.Gray, night = Color.Gray)
                        )
                    )
                }

                // Today's notes
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$todayCount",
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(day = Color(0xFF2E7D32), night = Color(0xFF81C784))
                        )
                    )
                    Text(
                        text = "Hôm nay",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = ColorProvider(day = Color.Gray, night = Color.Gray)
                        )
                    )
                }
            }
        }
    }
}

class StatsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatsWidget()
}
