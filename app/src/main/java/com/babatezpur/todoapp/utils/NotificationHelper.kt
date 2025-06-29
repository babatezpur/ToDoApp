package com.babatezpur.todoapp.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.babatezpur.todoapp.R
import com.babatezpur.todoapp.receiver.TodoActionReceiver
import com.babatezpur.todoapp.ui.activities.TodoViewActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "todo_reminder_channel"
        const val CHANNEL_NAME = "To-Do Reminders"
        const val CHANNEL_DESCRIPTION = "Channel for To-Do reminder notifications"

        const val NOTIFICATION_ID_BASE = 1000
    }

    init {
        // Create notification channel when helper is initialized
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // High importance for reminders
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }


    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showReminderNotification(todoId: Long, title: String, description: String, priority: String) {
        // Implementation for showing a reminder notification
        // This method would typically use NotificationCompat.Builder to create and display the notification
        // For example:
        try {
            val appIntent = Intent(context, TodoViewActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("todo_id", todoId)
                putExtra("from_notification", true)
            }

            val appPendingIntent = PendingIntent.getActivity(
                context, todoId.toInt(), appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val completeIntent = Intent(context, TodoActionReceiver::class.java).apply {
                action = TodoActionReceiver.ACTION_COMPLETE_TODO
                putExtra(TodoActionReceiver.EXTRA_TODO_ID, todoId)
            }

            val completePendingIntent = PendingIntent.getBroadcast(
                context, (todoId + 10000).toInt(), completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, TodoActionReceiver::class.java).apply {
                action = TodoActionReceiver.ACTION_SNOOZE_TODO
                putExtra(TodoActionReceiver.EXTRA_TODO_ID, todoId)
            }

            val snoozePendingIntent = PendingIntent.getBroadcast(
                context, (todoId + 20000).toInt(), snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )


            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("\"\uD83D\uDCCB Todo Reminder")
                .setContentText(title)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$title\n\n\$description")
                        .setBigContentTitle("Todo Reminder")
                        .setSummaryText("Tap to open • Swipe to dismiss")
                )
                .setSmallIcon(R.drawable.ic_notification_todo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
                .setContentIntent(appPendingIntent) // Set up a pending intent if needed
                .addAction(
                    R.drawable.ic_check_24,
                    "Complete",
                    completePendingIntent
                )
                .addAction(
                    R.drawable.ic_snooze_24,
                    "Snooze 15min",
                    snoozePendingIntent
                )
                .build()

            if (hasNotificationPermission()) {
                NotificationManagerCompat.from(context).notify(
                    NOTIFICATION_ID_BASE + todoId.toInt(),
                    notification
                )
                android.util.Log.d("NotificationHelper", "✅ Notification shown for todo: $todoId")
            } else {
                android.util.Log.w("NotificationHelper", "⚠️ Notification permission not granted")
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "NotificationHelper",
                "❌ Error showing notification for todo: $todoId",
                e
            )
        }
    }

    /**
     * 🎯 Cancels a specific notification
     *
     * WHEN USED: When todo is completed, deleted, or snoozed
     */
    fun cancelNotification(todoId: Long) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_BASE + todoId.toInt())
            android.util.Log.d("NotificationHelper", "🗑️ Notification canceled for todo: $todoId")
        } catch (e: Exception) {
            android.util.Log.e(
                "NotificationHelper",
                "❌ Failed to cancel notification: ${e.message}"
            )
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Notifications are enabled by default on older versions
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * 🎯 Checks if notifications are enabled in system settings
     *
     * WHY SEPARATE METHOD: User can disable notifications in settings even with permission
     */
    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}