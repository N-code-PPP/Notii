package com.example.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object ReminderNotificationHelper {
    const val CHANNEL_ID = "noted_reminders_channel"
    const val EXTRA_NOTE_ID = "extra_note_id"
    const val EXTRA_NOTE_TITLE = "extra_note_title"
    const val EXTRA_REMINDER_LABEL = "extra_reminder_label"
    const val EXTRA_IS_LOCKED = "extra_is_locked"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun scheduleNoteReminder(
        context: Context,
        noteId: String,
        noteTitle: String,
        reminderLabel: String,
        isLocked: Boolean,
        triggerAtMillis: Long
    ) {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(EXTRA_NOTE_ID, noteId)
            putExtra(EXTRA_NOTE_TITLE, noteTitle)
            putExtra(EXTRA_REMINDER_LABEL, reminderLabel)
            putExtra(EXTRA_IS_LOCKED, isLocked)
        }
        val requestCode = noteId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    const val ACTION_DISMISS_PERMANENT = "com.example.action.DISMISS_PERMANENT_NOTE_NOTIFICATION"

    private fun permanentNotificationId(noteId: String): Int = ("perm_$noteId").hashCode()

    fun cancelNoteReminder(context: Context, noteId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            noteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        cancelPermanentNoteNotification(context, noteId)
    }

    fun showPermanentNoteNotification(
        context: Context,
        noteId: String,
        noteTitle: String,
        noteContent: String,
        isLocked: Boolean
    ) {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTE_ID, noteId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            permanentNotificationId(noteId),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_DISMISS_PERMANENT
            putExtra(EXTRA_NOTE_ID, noteId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            permanentNotificationId(noteId) + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanTitle = noteTitle.trim()
        val displayBody = when {
            isLocked -> context.getString(R.string.locked_note_reminder_body)
            noteContent.isNotBlank() -> noteContent.trim()
            cleanTitle.isNotEmpty() -> cleanTitle
            else -> context.getString(R.string.default_reminder_body)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPendingIntent
            )

        val notification = if (cleanTitle.isNotEmpty()) {
            builder
                .setContentTitle(cleanTitle)
                .setContentText(displayBody)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(displayBody)
                        .setBigContentTitle(cleanTitle)
                )
                .build()
        } else {
            builder
                .setContentText(displayBody)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(displayBody)
                        .setBigContentTitle("")
                )
                .build()
        }

        try {
            NotificationManagerCompat.from(context).notify(permanentNotificationId(noteId), notification)
        } catch (ignored: SecurityException) {
        }
    }

    fun cancelPermanentNoteNotification(context: Context, noteId: String) {
        try {
            NotificationManagerCompat.from(context).cancel(permanentNotificationId(noteId))
        } catch (ignored: SecurityException) {
        }
    }

    fun showImmediateNotification(
        context: Context,
        noteId: String,
        noteTitle: String,
        reminderLabel: String,
        isLocked: Boolean
    ) {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTE_ID, noteId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            noteId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanTitle = noteTitle.trim()
        val displayBody = when {
            reminderLabel.isNotBlank() -> reminderLabel.trim()
            isLocked -> context.getString(R.string.locked_note_reminder_body)
            else -> context.getString(R.string.default_reminder_body)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        val notification = if (cleanTitle.isNotEmpty()) {
            builder
                .setContentTitle(cleanTitle)
                .setContentText(displayBody)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(displayBody)
                        .setBigContentTitle(cleanTitle)
                )
                .build()
        } else {
            builder
                .setContentText(displayBody)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(displayBody)
                        .setBigContentTitle("")
                )
                .build()
        }

        try {
            NotificationManagerCompat.from(context).notify(noteId.hashCode(), notification)
        } catch (ignored: SecurityException) {
        }
    }
}

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra(ReminderNotificationHelper.EXTRA_NOTE_ID) ?: return
        if (intent.action == ReminderNotificationHelper.ACTION_DISMISS_PERMANENT) {
            ReminderNotificationHelper.cancelPermanentNoteNotification(context, noteId)
            return
        }
        val noteTitle = intent.getStringExtra(ReminderNotificationHelper.EXTRA_NOTE_TITLE) ?: ""
        val reminderLabel = intent.getStringExtra(ReminderNotificationHelper.EXTRA_REMINDER_LABEL) ?: ""
        val isLocked = intent.getBooleanExtra(ReminderNotificationHelper.EXTRA_IS_LOCKED, false)

        ReminderNotificationHelper.showImmediateNotification(
            context = context,
            noteId = noteId,
            noteTitle = noteTitle,
            reminderLabel = reminderLabel,
            isLocked = isLocked
        )
    }
}
