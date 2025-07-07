package com.babatezpur.todoapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babatezpur.todoapp.data.entities.TodoStatistics
import com.babatezpur.todoapp.domain.managers.TodoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null,
    val statistics: TodoStatistics? = null
)


class SettingsViewModel(private val todoManager : TodoManager) : ViewModel() {
    // This ViewModel can be used to manage settings-related data and logic
    // For now, it can remain empty or contain basic settings management logic

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    /**
     * 📊 Load app statistics
     */
    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = todoManager.getTodoStatistics()
            result.fold(
                onSuccess = { stats ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statistics = stats,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load statistics: ${exception.message}"
                    )
                }
            )
        }
    }

    /**
     * 🗑️ Delete all todos (both active and completed)
     */
    fun deleteAllTodos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = todoManager.deleteAllTodos()
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "All todos deleted successfully! 🗑️",
                        error = null
                    )

                    loadStatistics()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to delete todos: ${exception.message}"
                    )
                }
            )
        }
    }

    /**
     * 🗑️ Delete only completed todos
     */
    fun deleteAllCompletedTodos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = todoManager.deleteAllCompletedTodos()
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "All completed todos deleted! ✅",
                        error = null
                    )

                    loadStatistics()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to delete completed todos: ${exception.message}"
                    )
                }
            )
        }
    }

    /**
     * 🔄 Clear messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            error = null
        )
    }
}