package com.babatezpur.todoapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babatezpur.todoapp.data.entities.Todo
import com.babatezpur.todoapp.domain.managers.TodoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CompletedTodosUIState(
    val completedTodos: List<Todo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)


class CompletedTodosViewModel(private val manager : TodoManager) : ViewModel() {
    private val _uiState = MutableStateFlow(CompletedTodosUIState())
    val uiState : StateFlow<CompletedTodosUIState> = _uiState.asStateFlow()

    init {
        loadCompletedTodos()
    }

    private fun loadCompletedTodos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                manager.getCompletedTodos().collect {todos ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        completedTodos = todos,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load completed todos"
                )
            }
        }
    }

    fun markTodoIncomplete(todo: Todo) {
        viewModelScope.launch {
            val result = manager.markTodoIncomplete(todo.id)

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Todo marked as incomplete",
                        error = null
                    )
                    // Optionally, reload the completed todos
                    loadCompletedTodos()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to mark todo as incomplete"
                    )
                }
            )
        }
    }

    /**
     * Refresh the completed todos list
     */
    fun refreshTodos() {
        loadCompletedTodos()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}