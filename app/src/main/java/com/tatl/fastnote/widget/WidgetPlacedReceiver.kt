package com.tatl.fastnote.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by the PendingIntent callback of
 * AppWidgetManager.requestPinAppWidget() when user confirms placement.
 *
 * Also exposes [startWatching] — a polling fallback for launchers (e.g. MIUI)
 * that place the widget silently without firing the PendingIntent callback.
 *
 * When widget is detected as active → go to home screen + start blink animation.
 */
class WidgetPlacedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Callback from launcher (Pixel, Samsung, etc.) — widget confirmed placed
        handleWidgetPlaced(context)
    }

    companion object {
        private const val PREFS_NAME       = "widget_highlight_prefs"
        private const val KEY_HIGHLIGHTED  = "is_highlighted"
        private const val KEY_HANDLED      = "placement_handled"

        // ── Public API ────────────────────────────────────────────

        /** Called from TripleActionWidget.provideGlance() to pick render state. */
        fun isHighlighted(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_HIGHLIGHTED, false)

        /**
         * Polls every 600 ms (up to 20 s) waiting for the widget to become
         * active on the home screen. Once detected → navigates home + starts
         * the blink animation.
         *
         * This handles launchers (MIUI, OnePlus, etc.) that place the widget
         * silently without firing the PendingIntent success callback.
         * A [KEY_HANDLED] flag prevents double-execution when the callback
         * fires first on Pixel/Samsung launchers.
         */
        fun startWatching(context: Context) {
            // Reset "handled" flag so this watch cycle is fresh
            setHandled(context, false)

            val handler = Handler(Looper.getMainLooper())
            val pollIntervalMs = 700L
            val maxAttempts   = 28          // ~20 seconds total

            var attempts = 0

            fun poll() {
                handler.postDelayed({
                    if (isHandled(context)) return@postDelayed   // callback already fired

                    val active = isWidgetActive(context)
                    if (active) {
                        handleWidgetPlaced(context)
                    } else {
                        attempts++
                        if (attempts < maxAttempts) poll()
                        // else: user likely cancelled — give up silently
                    }
                }, pollIntervalMs)
            }

            poll()
        }

        /** Start the blink animation (used by WidgetPlacedReceiver.startPulseAnimation). */
        fun startPulseAnimation(context: Context) {
            val handler = Handler(Looper.getMainLooper())
            val steps = 8               // 4 on/off cycles
            val intervalMs = 400L

            var step = 0

            fun scheduleNext() {
                handler.postDelayed({
                    setHighlighted(context, step % 2 == 0)
                    triggerWidgetRedraw(context)
                    step++
                    if (step < steps) scheduleNext()
                    else {
                        setHighlighted(context, false)
                        triggerWidgetRedraw(context)
                    }
                }, intervalMs)
            }

            scheduleNext()
        }

        // ── Private helpers ───────────────────────────────────────

        /**
         * Single entry point called when widget placement is confirmed
         * (either via PendingIntent callback or polling detection).
         */
        private fun handleWidgetPlaced(context: Context) {
            if (isHandled(context)) return      // prevent double-fire
            setHandled(context, true)

            // ✅ Đây là nơi DUY NHẤT đúng để mark widget đã được tạo thực sự
            // (sau khi launcher xác nhận đặt widget lên màn hình)
            com.tatl.fastnote.util.ThemePreferences.setWidgetPinned(true)

            // 1. Navigate to home screen so user lands on the widget
            goToHomeScreen(context)

            // 2. Brief delay to let home screen settle, then start blink
            Handler(Looper.getMainLooper()).postDelayed({
                startPulseAnimation(context)
            }, 500L)
        }

        private fun goToHomeScreen(context: Context) {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(intent) } catch (_: Exception) {}
        }

        private fun isWidgetActive(context: Context): Boolean {
            return try {
                val manager = android.appwidget.AppWidgetManager.getInstance(context)
                val provider = android.content.ComponentName(
                    context, TripleActionWidgetReceiver::class.java
                )
                manager.getAppWidgetIds(provider).isNotEmpty()
            } catch (_: Exception) { false }
        }

        private fun setHighlighted(context: Context, value: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_HIGHLIGHTED, value).apply()
        }

        private fun setHandled(context: Context, value: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_HANDLED, value).apply()
        }

        private fun isHandled(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_HANDLED, false)

        private fun triggerWidgetRedraw(context: Context) {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                val glanceManager = GlanceAppWidgetManager(context)
                val widget = TripleActionWidget()
                glanceManager.getGlanceIds(TripleActionWidget::class.java)
                    .forEach { id -> widget.update(context, id) }
            }
        }
    }
}
