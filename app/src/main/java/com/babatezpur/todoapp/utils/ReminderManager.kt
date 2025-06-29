package com.babatezpur.todoapp.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.AlarmManagerCompat.canScheduleExactAlarms
import com.babatezpur.todoapp.data.entities.Todo
import java.time.LocalDateTime
import java.time.ZoneOffset
import androidx.core.net.toUri
import com.babatezpur.todoapp.receiver.TodoReminderReceiver


/**
 * 🔔 ReminderManager - Handles alarm scheduling with AlarmManager
 *
 * FEATURES:
 * - Schedules exact alarms for precise reminder timing
 * - Handles Android 12+ exact alarm permissions
 * - Manages alarm cancellation and updates
 * - Provides fallback to inexact alarms when needed
 * - Bulk operations for multiple reminders
 */

class ReminderManager(private val context: Context) {

    companion object {
        private const val TAG = "ReminderManager"
        private const val REQUEST_CODE_BASE = 50000 // Base request code for alarms
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 🎯 Schedules a reminder alarm for the given todo
     *
     * PROCESS:
     * 1. Validates reminder time is in future
     * 2. Converts LocalDateTime to epoch milliseconds
     * 3. Creates intent with todo data
     * 4. Schedules exact or inexact alarm based on permissions
     * 5. Logs success/failure for debugging
     */

    fun scheduleReminder(todo : Todo){

        val reminderDateTime = todo.reminderDateTime
        if (reminderDateTime == null || reminderDateTime.isBefore(LocalDateTime.now())) {
            // Invalid reminder time, log error or handle it

            return
        }

        val triggerTimeMillis = reminderDateTime
            .atZone(java.time.ZoneId.systemDefault()) // Use device's local timezone
            .toInstant()
            .toEpochMilli()

        // ✅ INTENT CREATION: Create intent with todo data for receiver
        val intent = createReminderIntent(todo)
        val pendingIntent = createPendingIntent(todo.id, intent)

        try {
            if (canScheduleExactAlarms()) {
                // 📅 SCHEDULE EXACT ALARM: For Android 12+ with permission
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
                Log.d(
                    TAG,
                    "⏰ Exact alarm scheduled for todo: ${todo.id} - ${todo.title} at $reminderDateTime"
                )
            } else {
                // ⏰ FALLBACK TO INEXACT ALARM: For older versions or no permission
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
                Log.d(
                    TAG,
                    "⏰ Inexact alarm scheduled for todo: ${todo.id} - ${todo.title} at $reminderDateTime"
                )
                Log.w(TAG, "⚠️ Using inexact alarm - may be delayed by system")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to schedule reminder for todo: ${todo.id} - ${todo.title}", e)
            // Handle error (e.g. show notification, retry logic, etc.)
        }
    }

    /**
     * 🎯 Checks if the app can schedule exact alarms
     *
     * ANDROID VERSIONS:
     * - Android 12+: Requires special permission, can be revoked by user
     * - Android 11 and below: Always allowed
     *
     * WHY IMPORTANT: Exact alarms provide precise timing for reminders
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Exact alarms are allowed by default on older versions
        }
    }

    fun cancelReminder(todoId: Long){
        try {
            val intent = Intent(context, TodoReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getRequestCode(todoId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "🗑️ Reminder canceled for todo: $todoId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cancel reminder for todo: $todoId", e)
            // Handle error (e.g. show notification, retry logic, etc.)
        }
    }

    fun updateReminder(todo: Todo){
        Log.d(TAG, "🔄 Updating reminder for todo: ${todo.id}")

        // Cancel any existing reminder first
        cancelReminder(todo.id)

        // Schedule new reminder if reminder is still set
        if (todo.reminderDateTime != null) {
            scheduleReminder(todo)
            Log.d(TAG, "✅ Reminder updated for todo: ${todo.id}")
        } else {
            Log.d(TAG, "🚫 Reminder removed for todo: ${todo.id}")
        }
    }

    internal fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Request permission to schedule exact alarms
            // This requires user interaction, so handle accordingly
            Log.d(TAG, "Requesting permission to schedule exact alarms")
            try {
                val intent = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                ).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to open exact alarm settings", e)
                // Fallback: Open general app settings
                openAppSettings(context)
            }
        } else {
            Log.d(TAG, "✅ Exact alarm permission already granted or not required")
        }
    }

    private fun openAppSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = "package:${context.packageName}".toUri()
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to open app settings", e)
        }
    }

    /**
     * 🎯 Creates the intent for the reminder with todo data
     *
     * EXTRAS INCLUDED:
     * - Todo ID (primary key)
     * - Todo title (for quick display)
     * - Todo description (for notification body)
     * - Priority (for icon/styling)
     */
    private fun createReminderIntent(todo: Todo): Intent {
        return Intent(context, TodoReminderReceiver::class.java).apply {
            putExtra(TodoReminderReceiver.EXTRA_TODO_ID, todo.id)
            putExtra(TodoReminderReceiver.EXTRA_TODO_TITLE, todo.title)
            putExtra(TodoReminderReceiver.EXTRA_TODO_DESCRIPTION, todo.description)
            putExtra(TodoReminderReceiver.EXTRA_TODO_PRIORITY, todo.priority.name)
        }
    }


    /**
     * 🎯 Creates a pending intent for the reminder
     *
     * USAGE:
     * - Used by AlarmManager to trigger the receiver
     * - Unique request code based on todo ID to avoid conflicts
     */
    private fun createPendingIntent(todoId: Long, intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            getRequestCode(todoId), // Unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getRequestCode(todoId: Long): Int {
        return REQUEST_CODE_BASE + todoId.toInt()
    }



    // ===== BULK OPERATIONS =====

    /**
     * 🎯 Schedules multiple reminders efficiently
     *
     * USAGE: Boot receiver, bulk todo operations
     */
    fun scheduleReminders(todos: List<Todo>) {
        var successCount = 0
        todos.forEach { todo ->
            if (todo.reminderDateTime != null) {
                try {
                    scheduleReminder(todo)
                    successCount++
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to schedule reminder for todo: ${todo.id}", e)
                }
            }
        }
        Log.d(TAG, "📋 Scheduled $successCount/${todos.size} reminders")
    }

    /**
     * 🎯 Cancels multiple reminders efficiently
     *
     * USAGE: Bulk delete operations, user disabling all reminders
     */
    fun cancelReminders(todoIds: List<Long>) {
        var successCount = 0
        todoIds.forEach { todoId ->
            try {
                cancelReminder(todoId)
                successCount++
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to cancel reminder for todo: $todoId", e)
            }
        }
        Log.d(TAG, "🗑️ Canceled $successCount/${todoIds.size} reminders")
    }

    /**
     * 🎯 Gets information about alarm scheduling capabilities
     *
     * USAGE: Debugging, user feedback about why reminders might be delayed
     */
    fun getAlarmInfo(): String {
        return buildString {
            appendLine("🔔 Alarm Manager Info:")
            appendLine("• Can schedule exact alarms: ${canScheduleExactAlarms()}")
            appendLine("• Android version: ${Build.VERSION.SDK_INT}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                appendLine("• Exact alarm permission: ${if (canScheduleExactAlarms()) "Granted" else "Denied"}")
            }
        }
    }
}