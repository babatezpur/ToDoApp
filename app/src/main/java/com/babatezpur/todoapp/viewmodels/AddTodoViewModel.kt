package com.babatezpur.todoapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babatezpur.todoapp.domain.managers.TodoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

data class AddTodoUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)


class AddTodoViewModel(private val todoManager: TodoManager) : ViewModel() {
    // UI state

    private val _uiState = MutableStateFlow(AddTodoUiState())
    val uiState = _uiState

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
                onSuccess = { todoId ->
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
}