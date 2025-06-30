package com.babatezpur.todoapp.viewmodels

import androidx.lifecycle.ViewModel
import com.babatezpur.todoapp.data.entities.Todo
import androidx.lifecycle.viewModelScope
import androidx.room.util.query
import com.babatezpur.todoapp.domain.SortOption
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
    val completionMessage: String? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val currentSortOption: SortOption = SortOption.CREATED_DESC,
    val availableSortOptions: List<SortOption> = emptyList()
)

class TodoViewViewModel(private val todoManager: TodoManager) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoViewUiState())
    val uiState: StateFlow<TodoViewUiState> = _uiState.asStateFlow()

    init {
        initializeViewModel()
    }

    private fun initializeViewModel() {
        // Set available sort options
        _uiState.value = _uiState.value.copy(
            availableSortOptions = todoManager.getAvailableSortOptions()
        )

        // Load todos with default sorting
        loadTodosWithCurrentState()
    }

    private fun loadTodosWithCurrentState() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val currentState = _uiState.value
                todoManager.getTodosWithSearchAndSort(
                    query = currentState.searchQuery,
                    sortOption = currentState.currentSortOption
                ).collect { todos ->
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

    // SEARCH METHODS

    /**
     * Called when user clicks search icon - activates search mode
     */
    fun activateSearch() {
        _uiState.value = _uiState.value.copy(
            isSearchActive = true
        )
    }

    /**
     * Called when user closes search - deactivates and clears search
     */
    fun deactivateSearch() {
        _uiState.value = _uiState.value.copy(
            isSearchActive = false,
            searchQuery = ""
        )
        // Reload todos without search filter
        loadTodosWithCurrentState()
    }

    /**
     * Called as user types in search box - updates query and filters
     */
    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value

        // Only update if the query actually changed
        if (currentState.searchQuery != query) {
            _uiState.value = _uiState.value.copy(
                searchQuery = query
            )
            // Only reload if we're in search mode and query is different
            loadTodosWithCurrentState()
        }
    }

    /**
     * Clear search but keep search mode active
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = ""
        )
        loadTodosWithCurrentState()
    }

    // SORT METHODS

    /**
     * Called when user selects a sort option
     */
    fun updateSortOption(sortOption: SortOption) {
        _uiState.value = _uiState.value.copy(
            currentSortOption = sortOption
        )
        // Reload todos with new sort option
        loadTodosWithCurrentState()
    }

    /**
     * Get display name for current sort option (for UI)
     */
    fun getCurrentSortDisplayName(): String {
        return todoManager.getSortOptionDisplayName(_uiState.value.currentSortOption)
    }

    /**
     * Get display name for any sort option (for dialogs)
     */
    fun getSortDisplayName(sortOption: SortOption): String {
        return todoManager.getSortOptionDisplayName(sortOption)
    }


    fun markTodoCompleteWithDelay(todo: Todo) {
        viewModelScope.launch {
            try {
                delay(1500) // Show checked state for 1.5 seconds
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
                    _uiState.value = _uiState.value.copy(
                        completionMessage = "Todo deleted successfully! 🗑️"
                    )
                    // Todos will be automatically updated through the Flow
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
        loadTodosWithCurrentState()
    }


    // HELPER METHODS

    /**
     * Check if we have any active filters
     */
    fun hasActiveFilters(): Boolean {
        val currentState = _uiState.value
        return currentState.isSearchActive && currentState.searchQuery.isNotBlank()
    }

    /**
     * Reset all filters to default state
     */
    fun resetFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            isSearchActive = false,
            currentSortOption = SortOption.CREATED_DESC
        )
        loadTodosWithCurrentState()
    }


}