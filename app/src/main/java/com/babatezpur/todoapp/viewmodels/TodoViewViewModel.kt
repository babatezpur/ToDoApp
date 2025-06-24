package com.babatezpur.todoapp.viewmodels

import androidx.lifecycle.ViewModel
import com.babatezpur.todoapp.data.entities.Todo
import androidx.lifecycle.viewModelScope
import com.babatezpur.todoapp.domain.managers.TodoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TodoViewUiState(
    val isLoading: Boolean = false,
    val todos: List<Todo> = emptyList(),
    val error: String? = null
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

    fun markTodoComplete(todo: Todo) {
        viewModelScope.launch {
            val result = todoManager.markTodoComplete(todo.id)
            result.fold(
                onSuccess = {
                    // Successfully marked as complete, reload todos
                    loadTodos()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to mark todo as complete"
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