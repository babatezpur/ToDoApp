package com.babatezpur.todoapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.babatezpur.todoapp.data.database.TodoDatabase
import com.babatezpur.todoapp.data.repositories.TodoRepository
import com.babatezpur.todoapp.domain.managers.TodoManager
import com.babatezpur.todoapp.utils.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * 🎯 PRIMARY PURPOSE: Reschedules todo reminders after device reboot
 *
 * WHY THIS IS NEEDED:
 * - Android clears ALL AlarmManager alarms when device reboots
 * - Users expect their reminders to still work after restart
 * - This receiver runs automatically after boot to restore reminders
 *
 * WORKFLOW:
 * 1. Device boots up / app is updated
 * 2. Android triggers this receiver
 * 3. We fetch all todos with future reminders from database
 * 4. Reschedule each reminder with AlarmManager
 * 5. Users get their reminders back without knowing anything happened
 *
 * SECURITY NOTE: This only runs for apps the user has opened before
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    /**
     * 🚨 MAIN ENTRY POINT: Called by Android system after specific events
     *
     * TRIGGERS:
     * - ACTION_BOOT_COMPLETED: Device finished booting
     * - ACTION_MY_PACKAGE_REPLACED: This app was updated
     * - ACTION_PACKAGE_REPLACED: This app was updated (alternative trigger)
     *
     * WHY MULTIPLE TRIGGERS: Different Android versions/manufacturers use different events
     */
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "📱 Device rebooted or app updated - rescheduling reminders")
                rescheduleAllReminders(context)
            }
        }
    }

    /**
     * 🎯 REMINDER RESCHEDULER: Restores all pending reminders
     *
     * WHAT THIS DOES:
     * 1. Connects to database
     * 2. Finds all todos with future reminder times
     * 3. Filters out completed todos and past reminders
     * 4. Reschedules each valid reminder with AlarmManager
     *
     * IMPORTANT: This must be efficient since it runs on boot
     */
    private fun rescheduleAllReminders(context: Context) {
        // Keep receiver alive for async database work
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 🏗️ SETUP: Initialize database and managers
                val database = TodoDatabase.getDatabase(context)
                val repository = TodoRepository(database.todoDao())
                val todoManager = TodoManager(repository)
                val reminderManager = ReminderManager(context)

                // 📋 FETCH TODOS: Get all todos from database (one-time, not Flow)
                val todosResult = todoManager.getAllTodosDirect()

                todosResult.fold(
                    onSuccess = { todos ->
                        // 🔍 FILTER: Find todos that need reminder rescheduling
                        val todosWithReminders = todos.filter { todo ->
                            // Must be active (not completed)
                            !todo.isCompleted &&
                                    // Must have a reminder set
                                    todo.reminderDateTime != null &&
                                    // Reminder must be in the future
                                    todo.reminderDateTime.isAfter(LocalDateTime.now())
                        }

                        Log.d(TAG, "📊 Found ${todosWithReminders.size} todos with future reminders")

                        // 🔄 RESCHEDULE: Set up each reminder again
                        var successCount = 0
                        todosWithReminders.forEach { todo ->
                            try {
                                reminderManager.scheduleReminder(todo)
                                Log.d(TAG, "✅ Rescheduled reminder for todo: ${todo.id} - ${todo.title}")
                                successCount++
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Failed to reschedule reminder for todo: ${todo.id}", e)
                            }
                        }

                        Log.d(TAG, "🎉 Successfully rescheduled $successCount/${todosWithReminders.size} reminders")
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "💥 Failed to fetch todos for rescheduling: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error during reminder rescheduling: ${e.message}")
            } finally {
                // 🏁 CLEANUP: Always finish the async operation
                pendingResult.finish()
            }
        }
    }
}
