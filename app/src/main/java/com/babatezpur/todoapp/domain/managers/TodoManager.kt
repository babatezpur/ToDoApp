package com.babatezpur.todoapp.domain.managers

import com.babatezpur.todoapp.data.entities.Priority
import com.babatezpur.todoapp.data.entities.Todo
import com.babatezpur.todoapp.data.repositories.TodoRepository
import kotlinx.coroutines.flow.Flow
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

    suspend fun getAllActiveTodosDirect(): Result<List<Todo>> {
        return try {
            val todos = todoRepository.getAllActiveTodosDirect()
            Result.success(todos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTodo(
        title: String,
        description: String,
        priority: String, // Use String for simplicity, convert to enum in repository
        dueDateTime: LocalDateTime, // Store as epoch milliseconds
        reminderDateTime: LocalDateTime? = null, // Optional reminder
        isCompleted: Boolean = false
    ): Result<Long> {
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

            return Result.success(todoId)
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

