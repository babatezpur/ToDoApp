package com.babatezpur.todoapp.domain.managers

import com.babatezpur.todoapp.data.entities.Priority
import com.babatezpur.todoapp.data.entities.Todo
import com.babatezpur.todoapp.data.entities.TodoStatistics
import com.babatezpur.todoapp.data.repositories.TodoRepository
import com.babatezpur.todoapp.domain.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class TodoManager(private val todoRepository: TodoRepository) {

    fun getAllTodos(): Flow<List<Todo>> = todoRepository.getAllTodos()
    fun getTodoById(id: Long): Flow<Todo?> = todoRepository.getTodoById(id)

    // 📋 DIRECT METHODS (for receivers - one-time fetch with Result wrapper)
    suspend fun getTodoByIdDirect(id: Long): Result<Todo?> {
        return try {
            val todo = todoRepository.getTodoByIdDirect(id)
            Result.success(todo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllTodosDirect(): Result<List<Todo>> {
        return try {
            val todos = todoRepository.getAllTodosDirect()
            Result.success(todos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // get completed todos
    fun getCompletedTodos(): Flow<List<Todo>> = todoRepository.getCompletedTodos()

    suspend fun createTodo(
        title: String,
        description: String,
        priority: String, // Use String for simplicity, convert to enum in repository
        dueDateTime: LocalDateTime, // Store as epoch milliseconds
        reminderDateTime: LocalDateTime? = null, // Optional reminder
        isCompleted: Boolean = false
    ): Result<Todo> {
        try {
            // Business validation
            if (title.isBlank()) {
                return Result.failure(Exception("Title cannot be empty"))
            }

            if (dueDateTime.isBefore(LocalDateTime.now())) {
                return Result.failure(Exception("Due date cannot be in the past"))
            }

            reminderDateTime?.let { reminder ->
                if (reminder.isAfter(dueDateTime)) {
                    return Result.failure(Exception("Reminder cannot be after due date"))
                }
            }

            // Create the Todo object
            val todo = Todo(
                title = title,
                description = description,
                priority = when (priority) {
                    "High" -> Priority.P1
                    "Medium" -> Priority.P2
                    "Low" -> Priority.P3
                    else -> Priority.P2 // Default to Medium
                },
                dueDate = dueDateTime,
                reminderDateTime = reminderDateTime,
                isCompleted = isCompleted
            )

            val todoId = todoRepository.insertTodo(todo)

            reminderDateTime?.let {
                scheduleNotification(todoId, title, it)
            }

            todo.id = todoId // Set the ID from the database
            return Result.success(todo)
        } catch (e : Exception) {
            return Result.failure(e)
        }
    }

    suspend fun updateTodo(todo: Todo): Result<Unit> {
        return try {
            todoRepository.updateTodo(todo)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTodo(todo: Todo): Result<Unit> {
        return try {
            todoRepository.deleteTodo(todo)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mark complete with business logic
    suspend fun markTodoComplete(id: Long): Result<Unit> {
        try {
            todoRepository.markComplete(id)

            // TODO: Cancel notification when completed
            cancelNotification(id)

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    // 🗑️ BULK DELETE METHODS for Settings
    suspend fun deleteAllTodos(): Result<Unit> {
        return try {
            todoRepository.deleteAllTodos()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllCompletedTodos(): Result<Unit> {
        return try {
            todoRepository.deleteAllCompletedTodos()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllReminders(): Result<Unit> {
        return try {
            // Get all todos with reminders
            val todos = todoRepository.getAllTodosDirect()

            // Cancel each reminder
            todos.forEach { todo ->
                if (todo.reminderDateTime != null) {
                    cancelNotification(todo.id)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper methods for notifications (TODO: implement later)
    private fun scheduleNotification(todoId: Long, title: String, reminderDateTime: LocalDateTime) {
        // TODO: Schedule notification using WorkManager or AlarmManager
    }

    private fun cancelNotification(todoId: Long) {
        // TODO: Cancel scheduled notification
    }

    suspend fun markTodoIncomplete(id: Long): Result<Unit> {
        val result = try {
            todoRepository.markIncomplete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

        return result
    }


    fun getTodosWithSearchAndSort(
        query: String = "",
        sortOption: SortOption = SortOption.CREATED_DESC
    ): Flow<List<Todo>> {
        val cleanQuery = query.trim()

//        if (cleanQuery.length > 100) {
//            val truncatedQuery = cleanQuery.take(100)
//            return todoRepository.getTodosWithSearchAndSort(truncatedQuery, sortOption)
//        }
        return todoRepository.getTodosWithSearchAndSort(cleanQuery, sortOption)
    }

    fun searchTodos(searchQuery: String): Flow<List<Todo>> {
        val cleanQuery = searchQuery.trim()

        // Business validation: Minimum search length (optional)
        if (cleanQuery.isNotEmpty() && cleanQuery.isEmpty()) {
            // You could return empty list or the original query
            // For learning purposes, let's be permissive
        }

        return todoRepository.searchActiveTodos(cleanQuery)
    }

    fun getTodosSortedBy(sortOption: SortOption): Flow<List<Todo>> {
        return todoRepository.getTodosSortedBy(sortOption)
    }

    /**
     * Validates if a search query is acceptable
     * Returns Result<String> with cleaned query or error
     */
    fun validateSearchQuery(query: String): Result<String> {
        val cleanQuery = query.trim()

        return when {
            cleanQuery.length > 100 -> {
                Result.failure(Exception("Search query too long (max 100 characters)"))
            }
            cleanQuery.contains(Regex("[<>\"';]")) -> {
                // Basic injection protection (though Room should handle this)
                Result.failure(Exception("Search query contains invalid characters"))
            }
            else -> {
                Result.success(cleanQuery)
            }
        }
    }

    /**
     * Get display name for sort option (useful for UI)
     */
    fun getSortOptionDisplayName(sortOption: SortOption): String {
        return when (sortOption) {
            SortOption.CREATED_DESC -> "Newest First"
            SortOption.CREATED_ASC -> "Oldest First"
            SortOption.PRIORITY -> "Priority"
            SortOption.DUE_DATE_ASC -> "Due Date"
            SortOption.DUE_DATE_DESC -> "Due Date (Latest)"
        }
    }

    /**
     * Get all available sort options for UI display
     */
    fun getAvailableSortOptions(): List<SortOption> {
        return listOf(
            SortOption.CREATED_DESC,  // Default first
            SortOption.PRIORITY,
            SortOption.DUE_DATE_ASC
        )
    }

    /**
     * Log search activity (useful for debugging)
     */
    private fun logSearchActivity(query: String, resultCount: Int) {
        if (query.isNotBlank()) {
            android.util.Log.d("TodoManager", "Search '$query' returned $resultCount results")
        }
    }

    /**
     * Wrapper that adds logging to search
     */
    fun searchTodosWithLogging(searchQuery: String): Flow<List<Todo>> {
        return searchTodos(searchQuery).map { todos ->
            logSearchActivity(searchQuery, todos.size)
            todos
        }
    }

    // 📊 STATISTICS METHODS for Settings
    suspend fun getTodoStatistics(): Result<TodoStatistics> {
        return try {
            val total = todoRepository.getTotalTodosCount()
            val completed = todoRepository.getCompletedTodosCount()
            val active = todoRepository.getActiveTodosCount()

            val statistics = TodoStatistics(
                totalTodos = total,
                completedTodos = completed,
                activeTodos = active,
                completionRate = if (total > 0) (completed * 100) / total else 0
            )

            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }




    /**
    * 🔧 EXTENSION HELPER: Gets todos once instead of as Flow
    *
    * WHY NEEDED:
    * - BootReceiver needs one-time data fetch, not continuous Flow
    * - Flow would keep receiver alive indefinitely
    *
    */

    suspend fun TodoManager.getAllTodosOnce(): Result<List<Todo>> {
        return try {
            // Implementation needed in TodoManager:
            // Get todos from repository without Flow
            val todos = todoRepository.getAllTodosDirect() // Add this to repository
            Result.success(todos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

