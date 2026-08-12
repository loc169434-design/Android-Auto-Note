package com.tatl.fastnote.util

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.tatl.fastnote.widget.WidgetPlacedReceiver

/**
 * Utility helper to request 1-tap pinning of App Widgets
 * to the Android Home Screen (API 26+), and to check
 * whether a widget is currently active on the home screen.
 */
object PinWidgetHelper {

    /**
     * Request system to pin a specific AppWidget receiver to Home Screen.
     *
     * Two parallel strategies run after calling requestPinAppWidget:
     *
     *  A) PendingIntent callback — for Pixel/Samsung launchers that fire
     *     a broadcast when the user confirms placement in the system dialog.
     *     [WidgetPlacedReceiver.onReceive] → go home + blink animation.
     *
     *  B) Polling via [WidgetPlacedReceiver.startWatching] — for MIUI/OEM
     *     launchers that place the widget silently (no callback). Polls
     *     AppWidgetManager.getAppWidgetIds every 700 ms; when widget is
     *     detected → go home + blink animation.
     *
     * A [KEY_HANDLED] flag in SharedPreferences ensures only one path fires.
     * No manual navigation timer — we wait for actual widget placement.
     */
    fun pinWidget(context: Context, receiverClass: Class<*>, widgetName: String = "Widget") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val myProvider = ComponentName(context, receiverClass)

            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                // Strategy A: PendingIntent callback
                val callbackIntent = Intent(context, WidgetPlacedReceiver::class.java)
                val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val successCallback = PendingIntent.getBroadcast(
                    context, 0, callbackIntent, pendingFlags
                )
                appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)

                // Strategy B: Polling fallback (handles MIUI, OnePlus, etc.)
                // Runs in parallel — whichever fires first wins via KEY_HANDLED flag.
                WidgetPlacedReceiver.startWatching(context)

            } else {
                Toast.makeText(
                    context,
                    "Launcher không hỗ trợ ghim widget tự động — hãy kéo \"$widgetName\" thủ công từ menu Widget",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Hãy kéo \"$widgetName\" thủ công từ menu Widget màn hình chính",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Returns true if at least one instance of [receiverClass] widget
     * is currently placed on the home screen.
     */
    fun isWidgetActive(context: Context, receiverClass: Class<*>): Boolean {
        return try {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, receiverClass)
            manager.getAppWidgetIds(provider).isNotEmpty()
        } catch (e: Exception) {
            true
        }
    }
}
