package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class QuickCameraWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateCameraWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val KEY_PRIVATE_DOC_COUNT = "cached_private_doc_count"

        fun syncCameraWidget(context: Context, privateDocCount: Int) {
            val prefs = context.getSharedPreferences(NoteAppWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_PRIVATE_DOC_COUNT, privateDocCount).apply()

            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, QuickCameraWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                for (id in ids) {
                    updateCameraWidget(context, manager, id)
                }
            }
        }

        fun refreshAllWidgets(context: Context, privateDocCount: Int) {
            syncCameraWidget(context, privateDocCount)
        }

        fun updateCameraWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(NoteAppWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
            val docCount = prefs.getInt(KEY_PRIVATE_DOC_COUNT, 0)

            val views = RemoteViews(context.packageName, R.layout.widget_quick_camera)
            val subtitle = if (docCount > 0) {
                "$docCount private doc(s) • 10-shot capture"
            } else {
                context.getString(R.string.widget_quick_camera_subtitle)
            }
            views.setTextViewText(R.id.quick_camera_widget_subtitle, subtitle)

            val cameraIntent = Intent(context, MainActivity::class.java).apply {
                action = NoteAppWidgetProvider.ACTION_QUICK_CAMERA
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val cameraPending = PendingIntent.getActivity(
                context,
                1003,
                cameraIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.quick_camera_widget_root, cameraPending)
            views.setOnClickPendingIntent(R.id.quick_camera_widget_badge, cameraPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
