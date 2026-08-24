package com.tatl.fastnote.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import com.tatl.fastnote.billing.TrialManager
import com.tatl.fastnote.ui.ai.GeminiLaunchActivity
import com.tatl.fastnote.ui.fileviewer.FileViewerActivity
import com.tatl.fastnote.ui.recording.RecordingActivity

/**
 * Widget: Triple Action 1×3
 * Thiết kế OLED đen — container rounded xám tối, 3 SVG icon xám nhẹ.
 *
 * Click:
 *   🎙 → RecordingActivity   (bị chặn ngày 31+ nếu chưa premium)
 *   🧠 → GeminiLaunchActivity (bị chặn ngày 31+ nếu chưa premium)
 *   📓 → FileViewerActivity   (luôn mở)
 */
class TripleActionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val trialExpired = TrialManager.isTrialExpired(context)
        provideContent {
            GlanceTheme {
                WidgetContent(trialExpired = trialExpired)
            }
        }
    }

    @Composable
    private fun WidgetContent(trialExpired: Boolean) {
        // Outer box: TRONG SUỐT hoàn toàn — không có nền widget
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment   = Alignment.CenterVertically
            ) {
                // ── Mic — nút pill riêng biệt ────────────────────────────────
                PillIconButton(
                    iconRes            = R.drawable.ic_mic,
                    contentDescription = "Ghi âm",
                    modifier           = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(
                            if (trialExpired)
                                actionRunCallback<TrialExpiredCallback>()
                            else
                                actionStartActivity<RecordingActivity>()
                        )
                )

                Spacer(modifier = GlanceModifier.width(6.dp))

                // ── AI — nút pill riêng biệt ──────────────────────────────────
                PillIconButton(
                    iconRes            = R.drawable.ic_ai,
                    contentDescription = "AI",
                    modifier           = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(
                            if (trialExpired)
                                actionRunCallback<TrialExpiredCallback>()
                            else
                                actionStartActivity<GeminiLaunchActivity>()
                        )
                )

                Spacer(modifier = GlanceModifier.width(6.dp))

                // ── Note — nút pill riêng biệt ────────────────────────────────
                PillIconButton(
                    iconRes            = R.drawable.ic_note,
                    contentDescription = "Ghi chú",
                    modifier           = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(actionRunCallback<OpenNoteViewCallback>())
                )
            }
        }
    }

    /**
     * Nút icon trong suốt hoàn toàn (không nền), kích thước icon lớn 38dp
     */
    @Composable
    private fun PillIconButton(
        iconRes: Int,
        contentDescription: String,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Box(
            modifier = modifier
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider           = ImageProvider(iconRes),
                contentDescription = contentDescription,
                modifier           = GlanceModifier.size(38.dp)
            )
        }
    }
}

// ── Callback mở Màn hình Xem sổ ghi chú khi bấm nút Sổ trên Widget ────────────

class OpenNoteViewCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, com.tatl.fastnote.MainActivity::class.java).apply {
            action = "com.tatl.fastnote.ACTION_VIEW_NOTES"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("FROM_WIDGET_NOTE", true)
        }
        context.startActivity(intent)
    }
}

// ── Callback hiện thông báo khi trial hết hạn ────────────────────────────────

class TrialExpiredCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, com.tatl.fastnote.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("SHOW_TRIAL_EXPIRED", true)
        }
        context.startActivity(intent)
    }
}

class TripleActionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TripleActionWidget()
}
