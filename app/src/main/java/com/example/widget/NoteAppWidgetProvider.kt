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
import com.example.data.model.Note
import org.json.JSONArray
import org.json.JSONObject

class NoteAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val PREFS_NAME = "noted_widget_cache"
        const val KEY_NOTES_JSON = "cached_notes_json"
        const val KEY_TOTAL_COUNT = "cached_total_count"
        const val KEY_SYNC_STATUS = "cached_sync_status"
        const val ACTION_QUICK_NOTE = "com.example.ACTION_QUICK_NOTE"
        const val ACTION_VOICE_NOTE = "com.example.ACTION_VOICE_NOTE"
        const val ACTION_QUICK_CAMERA = "com.example.ACTION_QUICK_CAMERA"
        const val ACTION_OPEN_NOTE = "com.example.ACTION_OPEN_NOTE"

        fun syncWidgetCache(
            context: Context,
            notes: List<Note>,
            isOfflineOrPending: Boolean
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val topNotes = notes.take(3)
            val jsonArray = JSONArray()
            topNotes.forEach { note ->
                val obj = JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title.ifBlank { "Untitled Note" })
                    put(
                        "preview",
                        if (note.isLocked) "Locked Vault Note"
                        else note.content.replace("\n", " ").take(65)
                    )
                    put("category", note.category)
                    put("isPinned", note.isPinned)
                }
                jsonArray.put(obj)
            }
            prefs.edit()
                .putString(KEY_NOTES_JSON, jsonArray.toString())
                .putInt(KEY_TOTAL_COUNT, notes.size)
                .putString(KEY_SYNC_STATUS, if (isOfflineOrPending) "Offline Ready" else "Cloud Synced")
                .apply()

            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NoteAppWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                for (id in ids) {
                    updateAppWidget(context, manager, id)
                }
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val totalCount = prefs.getInt(KEY_TOTAL_COUNT, 0)
            val syncStatus = prefs.getString(KEY_SYNC_STATUS, "Cloud Synced") ?: "Cloud Synced"
            val rawJson = prefs.getString(KEY_NOTES_JSON, "[]") ?: "[]"

            val views = RemoteViews(context.packageName, R.layout.widget_noted)
            views.setTextViewText(R.id.widget_subtitle, "$totalCount notes • $syncStatus")

            val quickNoteIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_QUICK_NOTE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val quickNotePending = PendingIntent.getActivity(
                context,
                1001,
                quickNoteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_new_note, quickNotePending)

            val cameraShotIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_QUICK_CAMERA
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val cameraShotPending = PendingIntent.getActivity(
                context,
                1002,
                cameraShotIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_camera_shot, cameraShotPending)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPending = PendingIntent.getActivity(
                context,
                1000,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root_container, openAppPending)

            val parsed = try {
                JSONArray(rawJson)
            } catch (e: Exception) {
                JSONArray()
            }

            val titleIds = intArrayOf(R.id.widget_note_1_title, R.id.widget_note_2_title, R.id.widget_note_3_title)
            val bodyIds = intArrayOf(R.id.widget_note_1_body, R.id.widget_note_2_body, R.id.widget_note_3_body)

            for (i in 0 until 3) {
                if (i < parsed.length()) {
                    val obj = parsed.getJSONObject(i)
                    val title = obj.optString("title", "Untitled")
                    val category = obj.optString("category", "General")
                    val preview = obj.optString("preview", "")
                    views.setTextViewText(titleIds[i], "• [$category] $title")
                    views.setTextViewText(bodyIds[i], preview.ifBlank { "Tap to open note" })
                } else {
                    views.setTextViewText(
                        titleIds[i],
                        if (i == 0) "No pinned notes yet" else ""
                    )
                    views.setTextViewText(
                        bodyIds[i],
                        if (i == 0) "Tap '+ Quick' or '📷 Doc Shot' above to capture." else ""
                    )
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
