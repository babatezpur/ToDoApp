package com.babatezpur.todoapp.viewmodels

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babatezpur.todoapp.data.entities.Priority
import com.babatezpur.todoapp.data.entities.Todo
import com.babatezpur.todoapp.domain.managers.TodoManager
import com.babatezpur.todoapp.utils.NotificationHelper
import com.babatezpur.todoapp.utils.ReminderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class EditTodoUiState(
    val isLoading: Boolean = false,
    val todo: Todo? = null,
    val isUpdateSuccess: Boolean = false,
    val isDeleteSuccess: Boolean = false,
    val error: String? = null,
    val hasUnsavedChanges: Boolean = false
)

class EditTodoViewModel(
    private val todoManager: TodoManager,
    private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditTodoUiState())
    val uiState: StateFlow<EditTodoUiState> = _uiState.asStateFlow()

    private val reminderManager = ReminderManager(context)
    private val notificationHelper = NotificationHelper(context)

    private var originalTodo: Todo?  = null

    fun loadTodo(todoId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                todoManager.getTodoById(todoId).collect { todo ->
                    if(todo != null) {
                        originalTodo = todo
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            todo = todo,
                            error = null,
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Todo not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load todo: ${e.message}"
                )
            }
        }
    }

    fun updateTodo(
        title: String,
        description: String,
        priority: String,
        dueDateTime: LocalDateTime,
        reminderDateTime: LocalDateTime? = null,
    ) {
        viewModelScope.launch {
            val currentTodo = _uiState.value.todo
            if (currentTodo == null) {
                _uiState.value = _uiState.value.copy(error = "No todo to update")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val priorityEnum = when(priority) {
                "High" -> Priority.P1
                "Medium" -> Priority.P2
                "Low" -> Priority.P3
                else -> Priority.P2 // Default to Medium
            }

            val updatedTodo = currentTodo.copy(
                title = title,
                description = description,
                priority = priorityEnum,
                dueDate = dueDateTime,
                reminderDateTime = reminderDateTime
            )

            val result = todoManager.updateTodo(updatedTodo)

            result.fold(
                onSuccess = {

                    handleReminderUpdate(originalTodo, updatedTodo)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isUpdateSuccess = true,
                        hasUnsavedChanges = false
                    )
//                    originalTodo = updatedTodo // Update original state
//                    reminderDateTime?.let {
//                        reminderManager.scheduleReminder(updatedTodo)
//                    }
//                    notificationHelper.showNotification(
//                        title = "Todo Updated",
//                        message = "Your todo '${updatedTodo.title}' has been updated."
//                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to update todo: ${exception.message}"
                    )
                }
            )

        }
    }

    fun deleteTodo() {
        viewModelScope.launch {
            val currentTodo = _uiState.value.todo
            if (currentTodo == null) {
                _uiState.value = _uiState.value.copy(error = "No todo to delete")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = todoManager.deleteTodo(currentTodo)

            result.fold(
                onSuccess = {
                    // Cancel any existing reminder
                    if (currentTodo.reminderDateTime != null) {
                        reminderManager.cancelReminder(currentTodo.id)
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isDeleteSuccess = true
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to delete todo: ${exception.message}"
                    )
                }
            )
        }
    }

    /**
     * Check if current form data differs from original todo
     */
    fun checkForUnsavedChanges(
        title: String,
        description: String,
        priority: String,
        dueDateTime: LocalDateTime,
        reminderDateTime: LocalDateTime?
    ) {
        val currentTodo = originalTodo ?: return

        val priorityEnum = when (priority) {
            "High" -> Priority.P1
            "Medium" -> Priority.P2
            "Low" -> Priority.P3
            else -> Priority.P2 // Default to Medium
        }

        val hasChanges = currentTodo.title != title ||
                currentTodo.description != description ||
                currentTodo.priority != priorityEnum ||
                currentTodo.dueDate != dueDateTime ||
                currentTodo.reminderDateTime != reminderDateTime

        _uiState.value = _uiState.value.copy(hasUnsavedChanges = hasChanges)
    }

    /**
    * Handle reminder updates (cancel old, schedule new)
    */
    private suspend fun handleReminderUpdate(oldTodo: Todo?, newTodo: Todo) {
        try {
            // Cancel old reminder if it existed
            oldTodo?.let { old ->
                if (old.reminderDateTime != null) {
                    reminderManager.cancelReminder(old.id)
                }
            }

            // Schedule new reminder if it exists
            if (newTodo.reminderDateTime != null) {
                if (!notificationHelper.hasNotificationPermission()) {
                    Toast.makeText(context, "Please enable notifications for reminders", Toast.LENGTH_LONG).show()
                }

                if (!reminderManager.canScheduleExactAlarms()) {
                    Toast.makeText(context, "Please enable exact alarms for precise reminders", Toast.LENGTH_LONG).show()
                } else {
                    reminderManager.scheduleReminder(newTodo)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EditTodoViewModel", "Failed to update reminder", e)
        }
    }


    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Reset success states (called after showing success message)
     */
    fun resetSuccessStates() {
        _uiState.value = _uiState.value.copy(
            isUpdateSuccess = false,
            isDeleteSuccess = false
        )
    }


    // ===== VALIDATION METHODS (reuse from AddTodoViewModel) =====

    /**
     * Validate due date/time
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
     * Validate reminder time
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
     * Validate that reminder time is before due time
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

    /**
     * Check reminder permissions
     */
    fun checkReminderPermissions(): Boolean {
        return notificationHelper.hasNotificationPermission() && reminderManager.canScheduleExactAlarms()
    }

    /**
     * Request reminder permissions
     */
    fun requestReminderPermissions() {
        try {
            reminderManager.requestExactAlarmPermission()
        } catch (e: Exception) {
            android.util.Log.e("EditTodoViewModel", "Failed to request permissions", e)
        }
    }


}