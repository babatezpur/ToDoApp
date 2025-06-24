package com.babatezpur.todoapp.viewmodels

import androidx.lifecycle.ViewModel
import com.babatezpur.todoapp.data.entities.Todo
import androidx.lifecycle.viewModelScope
import com.babatezpur.todoapp.domain.managers.TodoManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TodoViewUiState(
    val isLoading: Boolean = false,
    val todos: List<Todo> = emptyList(),
    val error: String? = null,
    val completionMessage: String? = null
)

class TodoViewViewModel(private val todoManager: TodoManager) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoViewUiState())
    val uiState: StateFlow<TodoViewUiState> = _uiState.asStateFlow()

    init {
        loadTodos()
    }

    private fun loadTodos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                todoManager.getAllTodos().collect { todos ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        todos = todos,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun markTodoCompleteWithDelay(todo: Todo) {
        viewModelScope.launch {
            try {
                // ✅ Use coroutine delay instead of Thread.sleep
                delay(1500) // Show checked state for 1.5 seconds

                // Mark as complete in database
                val result = todoManager.markTodoComplete(todo.id)

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            completionMessage = "Todo completed! ✓"
                        )
                        // Todos will be automatically updated through the Flow
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            error = exception.message ?: "Failed to mark todo as complete"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun toggleTodoCompletion(todo: Todo) {
        viewModelScope.launch {
            val result = if (todo.isCompleted) {
                todoManager.markTodoIncomplete(todo.id)
            } else {
                todoManager.markTodoComplete(todo.id)
            }

            result.fold(
                onSuccess = {
                    val message = if (todo.isCompleted) {
                        "Todo marked as incomplete"
                    } else {
                        "Todo completed! ✓"
                    }
                    _uiState.value = _uiState.value.copy(
                        completionMessage = message
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to update todo: ${exception.message}"
                    )
                }
            )
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            val result = todoManager.deleteTodo(todo)
            result.fold(
                onSuccess = {
                    // Successfully deleted, reload todos
                    loadTodos()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to delete todo"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refreshTodos() {
        loadTodos()
    }


}