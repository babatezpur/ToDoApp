package com.babatezpur.todoapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.babatezpur.todoapp.data.database.TodoDatabase
import com.babatezpur.todoapp.data.repositories.TodoRepository
import com.babatezpur.todoapp.domain.managers.TodoManager
import com.babatezpur.todoapp.utils.NotificationHelper
import com.babatezpur.todoapp.utils.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TodoActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_COMPLETE_TODO = "com.babatezpur.todoapp.COMPLETE_TODO"
        const val ACTION_SNOOZE_TODO = "com.babatezpur.todoapp.ACTION_SNOOZE_TODO"
        const val EXTRA_TODO_ID = "todo_id"

        private const val TAG = "TodoActionReceiver"
        private const val SNOOZE_MINUTES = 15L // Default snooze time in minutes
    }
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        if (todoId == -1L) {
            // Invalid todo ID, log an error or handle it appropriately
            return
        }

        val action = intent.action
        Log.d(TAG, "🔔 Action received: $action for todo: $todoId")

        // Keep receiver alive for async work
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initialize dependencies
                val database = TodoDatabase.getDatabase(context)
                val repository = TodoRepository(database.todoDao())
                val todoManager = TodoManager(repository)
                val notificationHelper = NotificationHelper(context)
                val reminderManager = ReminderManager(context)

                // Route to appropriate handler based on action
                when (action) {
                    ACTION_COMPLETE_TODO -> {
                        handleCompleteTodo(
                            todoId, todoManager, notificationHelper,
                            reminderManager, context
                        )
                    }
                    ACTION_SNOOZE_TODO -> {
                        handleSnoozeTodo(
                            todoId, todoManager, notificationHelper,
                            reminderManager, context
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error handling action: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * 🎯 COMPLETE TODO HANDLER: Marks todo as completed
     *
     * WHAT THIS DOES:
     * 1. Marks todo as complete in database
     * 2. Cancels any future reminders for this todo
     * 3. Dismisses the current notification
     * 4. Shows success toast to user
     *
     * WHY CANCEL REMINDERS: Completed todos shouldn't have future reminders
     */
    private suspend fun handleCompleteTodo(
        todoId: Long,
        todoManager: TodoManager,
        notificationHelper: NotificationHelper,
        reminderManager: ReminderManager,
        context: Context
    ) {
        Log.d(TAG, "✅ Completing todo from notification: $todoId")

        val result = todoManager.markTodoComplete(todoId)
        result.fold(
            onSuccess = {
                // 🚫 CANCEL REMINDERS: No future reminders needed for completed todo
                reminderManager.cancelReminder(todoId)

                // 🗑️ DISMISS NOTIFICATION: Remove from notification bar
                notificationHelper.cancelNotification(todoId)

                // ✅ SUCCESS FEEDBACK: Show user confirmation on main thread
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Todo completed! ✓", Toast.LENGTH_SHORT).show()
                }

                Log.d(TAG, "🎉 Todo $todoId marked as complete from notification")
            },
            onFailure = { exception ->
                Log.e(TAG, "💥 Failed to complete todo $todoId: ${exception.message}")

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Failed to complete todo", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    /**
     * 🎯 SNOOZE TODO HANDLER: Reschedules reminder for later
     *
     * WHAT THIS DOES:
     * 1. Gets current todo from database
     * 2. Calculates new reminder time (current time + snooze minutes)
     * 3. Updates todo with new reminder time
     * 4. Schedules new alarm for snooze time
     * 5. Dismisses current notification
     * 6. Shows snooze confirmation
     *
     * WHY UPDATE DATABASE: Keeps reminder time in sync with what user expects
     */
    private suspend fun handleSnoozeTodo(
        todoId: Long,
        todoManager: TodoManager,
        notificationHelper: NotificationHelper,
        reminderManager: ReminderManager,
        context: Context
    ) {
        Log.d(TAG, "😴 Snoozing todo from notification: $todoId")

        try {
            // 📋 GET TODO: Fetch current todo details
            val todoResult = todoManager.getTodoByIdDirect(todoId)

            todoResult.fold(
                onSuccess = { todo ->
                    if (todo != null && !todo.isCompleted) {
                        // ⏰ CALCULATE NEW TIME: Current time + snooze duration
                        val newReminderTime = LocalDateTime.now().plusMinutes(SNOOZE_MINUTES)

                        // 💾 UPDATE DATABASE: Save new reminder time
                        val updatedTodo = todo.copy(reminderDateTime = newReminderTime)
                        val updateResult = todoManager.updateTodo(updatedTodo)

                        updateResult.fold(
                            onSuccess = {
                                // 🔄 SCHEDULE NEW ALARM: Set alarm for snooze time
                                reminderManager.scheduleReminder(updatedTodo)

                                // 🗑️ DISMISS CURRENT NOTIFICATION
                                notificationHelper.cancelNotification(todoId)

                                // 😴 SUCCESS FEEDBACK: Confirm snooze
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(
                                        context,
                                        "Reminder snoozed for $SNOOZE_MINUTES minutes",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                Log.d(TAG, "😴 Todo $todoId snoozed until $newReminderTime")
                            },
                            onFailure = { exception ->
                                Log.e(TAG, "💥 Failed to update todo for snooze: ${exception.message}")
                                showSnoozeError(context)
                            }
                        )
                    } else {
                        Log.d(TAG, "⚠️ Todo $todoId is completed or doesn't exist")
                        notificationHelper.cancelNotification(todoId)
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "💥 Failed to get todo for snooze: ${exception.message}")
                    showSnoozeError(context)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error during snooze operation: ${e.message}")
            showSnoozeError(context)
        }
    }

    /**
     * 🎯 ERROR HELPER: Shows snooze error message
     *
     * WHAT THIS DOES: Displays error toast on main thread
     * WHY SEPARATE METHOD: Reused by multiple error paths
     */
    private fun showSnoozeError(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, "Failed to snooze reminder", Toast.LENGTH_SHORT).show()
        }
    }
}