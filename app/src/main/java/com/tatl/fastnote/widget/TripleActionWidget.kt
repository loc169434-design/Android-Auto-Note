package com.tatl.fastnote.widget

import android.appwidget.AppWidgetManager
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
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
import com.tatl.fastnote.util.PinWidgetHelper
import com.tatl.fastnote.util.ThemePreferences

/**
 * Widget: Triple Action 1×3
 * Thiết kế OLED đen — 3 SVG icon (Micro + Ghi chú + AI).
 * Tự động thích ứng kích thước (SizeMode.Exact) trên các kích cỡ màn hình khác nhau.
 *
 * Click:
 *   🎙 → RecordingActivity   (bị chặn ngày 31+ nếu chưa premium)
 *   🧠 → GeminiLaunchActivity (bị chặn ngày 31+ nếu chưa premium)
 *   📓 → FileViewerActivity   (luôn mở)
 */
class TripleActionWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

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
        val size = LocalSize.current

        // Tính toán kích thước icon linh hoạt theo kích thước thực tế của widget trên launcher
        val iconSize = if (size.width > 0.dp && size.height > 0.dp) {
            val availableHeight = size.height - 8.dp
            val availableWidthPerItem = (size.width - 20.dp) / 3
            minOf(availableHeight * 0.85f, availableWidthPerItem * 0.85f).coerceIn(32.dp, 54.dp)
        } else {
            46.dp
        }

        val spacerWidth = if (size.width > 0.dp && size.width < 180.dp) 2.dp else 4.dp

        // Outer box: TRONG SUỐT hoàn toàn — không có nền widget
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment   = Alignment.CenterVertically
            ) {
                // ── 1. Mic — nút pill bên trái ──────────────────────────────
                PillIconButton(
                    iconRes            = R.drawable.ic_mic,
                    contentDescription = "Ghi âm",
                    iconSize           = iconSize,
                    modifier           = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(actionStartActivity<RecordingActivity>())
                )

                Spacer(modifier = GlanceModifier.width(spacerWidth))

                // ── 2. Sổ (Note) — nút pill ở giữa ──────────────────────────
                PillIconButton(
                    iconRes            = R.drawable.ic_note,
                    contentDescription = "Ghi chú",
                    iconSize           = iconSize,
                    modifier           = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .clickable(actionRunCallback<OpenNoteViewCallback>())
                )

                Spacer(modifier = GlanceModifier.width(spacerWidth))

                // ── 3. Não (AI) — nút pill bên phải ──────────────────────────
                PillIconButton(
                    iconRes            = R.drawable.ic_ai,
                    contentDescription = "AI",
                    iconSize           = iconSize,
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
            }
        }
    }

    /**
     * Không nền (trong suốt hoàn toàn), icon tự co giãn theo kích thước widget.
     */
    @Composable
    private fun PillIconButton(
        iconRes: Int,
        contentDescription: String,
        iconSize: androidx.compose.ui.unit.Dp,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider           = ImageProvider(iconRes),
                contentDescription = contentDescription,
                modifier           = GlanceModifier.size(iconSize)
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
        // Nếu chưa có ghi chú nào → mở thẳng Recording thay vì Home trống
        val guidiFile = com.tatl.fastnote.util.FileHelper.getGuidiFile(context)
        val hasNotes = guidiFile.exists() && guidiFile.length() > 10

        val intent = if (hasNotes) {
            Intent(context, com.tatl.fastnote.MainActivity::class.java).apply {
                action = "com.tatl.fastnote.ACTION_VIEW_NOTES"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("FROM_WIDGET_NOTE", true)
            }
        } else {
            Intent(context, RecordingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_DISABLED -> {
                // Toàn bộ instance widget đã bị xoá khỏi launcher
                ThemePreferences.setWidgetPinned(false)
            }
            AppWidgetManager.ACTION_APPWIDGET_DELETED -> {
                val isActive = PinWidgetHelper.isWidgetActive(context, TripleActionWidgetReceiver::class.java)
                if (!isActive) {
                    ThemePreferences.setWidgetPinned(false)
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_ENABLED -> {
                ThemePreferences.setWidgetPinned(true)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val isActive = PinWidgetHelper.isWidgetActive(context, TripleActionWidgetReceiver::class.java)
        if (!isActive) {
            ThemePreferences.setWidgetPinned(false)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        ThemePreferences.setWidgetPinned(false)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ThemePreferences.setWidgetPinned(true)
    }
}
