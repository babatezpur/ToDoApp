package com.babatezpur.todoapp.viewmodels

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babatezpur.todoapp.data.entities.Todo
import com.babatezpur.todoapp.domain.managers.TodoManager
import com.babatezpur.todoapp.utils.NotificationHelper
import com.babatezpur.todoapp.utils.ReminderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

data class AddTodoUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)


class AddTodoViewModel(private val todoManager: TodoManager, private val context: Context) : ViewModel() {
    // UI state

    private val _uiState = MutableStateFlow(AddTodoUiState())
    val uiState : StateFlow<AddTodoUiState> = _uiState.asStateFlow()

    private val reminderManager = ReminderManager(context)
    private val notificationHelper = NotificationHelper(context)


    fun createTodo(
        title: String,
        description: String,
        priority: String,
        dueDateTime: LocalDateTime, // Store as epoch milliseconds
        reminderDateTime: LocalDateTime? = null // Optional reminder
    ) {
        viewModelScope.launch {

            // Reset UI state
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = todoManager.createTodo(
                title = title,
                description = description,
                priority = priority,
                dueDateTime = dueDateTime,
                reminderDateTime = reminderDateTime,
            )

            result.fold(
                onSuccess = { createdTodo ->

                    // Schedule reminder if provided
                    var reminderScheduled = false
                    if (createdTodo.reminderDateTime != null) {
                        reminderScheduled = scheduleReminderForTodo(createdTodo)
                    }

                    // Successfully created Todo
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                },
                onFailure = { exception ->
                    // Handle error
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = false,
                        error = exception.message ?: "Unknown error"
                    )
                }
            )
        }
    }

    fun clearError() {
        // Clear any error messages
        _uiState.value = _uiState.value.copy(error = null)
    }

    private suspend fun scheduleReminderForTodo(todo: Todo): Boolean {
        return try {
            // Schedule the reminder using ReminderManager
            if (!notificationHelper.hasNotificationPermission()) {
                Toast.makeText(context, "Please enable notifications permission in settings to use reminders.", Toast.LENGTH_LONG).show()
            }

            if(!reminderManager.canScheduleExactAlarms()) {
                // Show a toast asking for alarm permission
                Toast.makeText(context,
                    "Please enable 'Schedule exact alarms' permission in settings to use reminders.",
                    Toast.LENGTH_LONG
                ).show()
                return false
            }

            reminderManager.scheduleReminder(todo)

            true
        } catch (e: Exception) {
            // Handle any errors in scheduling
            android.util.Log.e("AddTodoViewModel", "❌ Failed to schedule reminder for todo: ${todo.id}", e)

            // Update UI state to show reminder scheduling failed
            _uiState.value = _uiState.value.copy(
                error = "Todo saved, but reminder scheduling failed: ${e.message}"
            )
            false
        }
    }


    // ===== PERMISSION CHECKING =====

    /**
     * 🔒 Check if reminder permissions are available
     */
    fun checkReminderPermissions(): Boolean {
        val hasNotificationPermission = notificationHelper.hasNotificationPermission()
        val canScheduleExactAlarms = reminderManager.canScheduleExactAlarms()

        android.util.Log.d("AddTodoViewModel", "Permission check - Notifications: $hasNotificationPermission, Exact alarms: $canScheduleExactAlarms")

        return hasNotificationPermission && canScheduleExactAlarms
    }

    /**
     * 🔒 Request reminder permissions if needed
     */
    fun requestReminderPermissions() {
        try {
            reminderManager.requestExactAlarmPermission()
        } catch (e: Exception) {
            android.util.Log.e("AddTodoViewModel", "❌ Failed to request permissions: ${e.message}")
        }
    }

    // ===== VALIDATION METHODS =====

    /**
     * 🔍 Validate due date/time
     */
    fun validateDueDateTime(dueDateTime: LocalDateTime): String? {
        val now = LocalDateTime.now()

        return when {
            dueDateTime.isBefore(now.minusMinutes(1)) ->
                "Due time cannot be in the past"
            dueDateTime.isAfter(now.plusYears(5)) ->
                "Due time cannot be more than 5 years in the future"
            else -> null
        }
    }

    /**
     * 🔍 Validate reminder time
     */
    fun validateReminderTime(reminderDateTime: LocalDateTime?): String? {
        if (reminderDateTime == null) return null

        val now = LocalDateTime.now()

        return when {
            reminderDateTime.isBefore(now) ->
                "Reminder time cannot be in the past"
            reminderDateTime.isAfter(now.plusYears(1)) ->
                "Reminder time cannot be more than a year in the future"
            else -> null
        }
    }

    /**
     * 🔍 Validate that reminder time is before due time
     */
    fun validateReminderBeforeDue(
        reminderDateTime: LocalDateTime?,
        dueDateTime: LocalDateTime
    ): String? {
        if (reminderDateTime == null) return null

        return if (reminderDateTime.isAfter(dueDateTime)) {
            "Reminder time must be before due time"
        } else if (reminderDateTime.isEqual(dueDateTime)) {
            "Reminder time should be before due time for better planning"
        } else null
    }
}