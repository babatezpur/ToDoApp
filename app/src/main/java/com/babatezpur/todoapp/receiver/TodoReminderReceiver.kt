package com.babatezpur.todoapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.babatezpur.todoapp.data.database.TodoDatabase
import com.babatezpur.todoapp.data.repositories.TodoRepository
import com.babatezpur.todoapp.domain.managers.TodoManager
import com.babatezpur.todoapp.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.launch

class TodoReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_TODO_TITLE = "todo_title"
        const val EXTRA_TODO_DESCRIPTION = "todo_description"
        const val EXTRA_TODO_PRIORITY = "todo_priority"

        private const val TAG = "TodoReminderReceiver"

    }

    override fun onReceive(context: Context, intent: Intent) {


        // Handle the reminder action here
        // You can extract data from the intent and perform necessary actions
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        if (todoId == -1L) {
            Log.e(TAG, "❌ Invalid todo ID received - alarm was malformed")
            return
        }

        Log.d(TAG, "Received reminder for Todo ID: $todoId")

        val title = intent.getStringExtra(EXTRA_TODO_TITLE) ?: "Todo Reminder"
        val description = intent.getStringExtra(EXTRA_TODO_DESCRIPTION) ?: ""
        val priority = intent.getStringExtra(EXTRA_TODO_PRIORITY) ?: "P2"

        Log.d(TAG, "Todo Reminder: $title - $description (Priority: $priority)")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = TodoDatabase.getDatabase(context)
                val repository = TodoRepository(database.todoDao())
                val todoManager = TodoManager(repository)

                val todoResult = todoManager.getTodoByIdDirect(todoId)

                todoResult.fold(
                    onSuccess = { todo ->
                        if (todo != null && !todo.isCompleted) {
                            Log.d(TAG, "Showing notification for active todo: ${todo.title}")
                            val notificationHelper = NotificationHelper(context)
                            notificationHelper.showReminderNotification(
                                todoId = todo.id,
                                title = todo.title,
                                description = todo.description,
                                priority = todo.priority.toString(),
                            )
                        } else {
                            Log.d(TAG, "⚠️ Todo $todoId is completed or deleted - skipping notification")
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error fetching Todo: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in TodoReminderReceiver: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }

    }
}