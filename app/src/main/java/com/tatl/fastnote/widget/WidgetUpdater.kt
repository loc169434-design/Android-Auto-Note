package com.tatl.fastnote.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

/**
 * Helper to update all widgets when data changes (e.g., after saving a new note).
 */
object WidgetUpdater {
    suspend fun updateAllWidgets(context: Context) {
        try {
            val manager = GlanceAppWidgetManager(context)

            // Update Quick Record Widget
            val quickRecordIds = manager.getGlanceIds(QuickRecordWidget::class.java)
            quickRecordIds.forEach { id ->
                QuickRecordWidget().update(context, id)
            }

            // Update Recent Notes Widget
            val recentNotesIds = manager.getGlanceIds(RecentNotesWidget::class.java)
            recentNotesIds.forEach { id ->
                RecentNotesWidget().update(context, id)
            }

            // Update Stats Widget
            val statsIds = manager.getGlanceIds(StatsWidget::class.java)
            statsIds.forEach { id ->
                StatsWidget().update(context, id)
            }

            // Update Open Notes Widget
            val openNotesIds = manager.getGlanceIds(OpenNotesWidget::class.java)
            openNotesIds.forEach { id ->
                OpenNotesWidget().update(context, id)
            }

            // Update Triple Action Widget
            val tripleActionIds = manager.getGlanceIds(TripleActionWidget::class.java)
            tripleActionIds.forEach { id ->
                TripleActionWidget().update(context, id)
            }
        } catch (e: Exception) {
            // Widget might not be placed on home screen — safe to ignore
        }
    }
}
