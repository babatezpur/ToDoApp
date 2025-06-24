package com.babatezpur.todoapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.babatezpur.todoapp.data.entities.Todo
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TodoDao {

    @Insert
    suspend fun insertTodo(todo: Todo) : Long

    @Delete
    suspend fun deleteTodo(todo: Todo)

    @Delete
    suspend fun deleteTodos(todos: List<Todo>)

    @Update
    suspend fun updateTodo(todo: Todo)


    @Query("SELECT * FROM todos WHERE id = :todoId")
    fun getTodoById(todoId: Long): Flow<Todo?>

    // Primary method for the main To-Do list screen - shows current work
    @Query("SELECT * FROM todos WHERE is_completed = 0 ORDER BY created_at DESC")
    fun getActiveTodosByCreationDate(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE is_completed = 0 ORDER BY due_date ASC, priority ASC")
    fun getActiveTodosSortedByDueDate(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE due_date <= :currentDateTime AND is_completed = 0 ORDER BY due_date ASC")
    fun getOverdueTodos(currentDateTime: LocalDateTime = LocalDateTime.now()): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE is_completed = 0 ORDER BY due_date ASC")
    fun getActiveTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE is_completed = 1 ORDER BY due_date ASC")
    fun getCompletedTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE is_completed = 0 ORDER BY priority ASC, due_date ASC")
    fun getActiveTodosByPriority(): Flow<List<Todo>>


    // Mark complete/incomplete by ID
    @Query("""
        UPDATE todos 
        SET is_completed = :isCompleted, 
            updated_at = :updatedAt
        WHERE id = :todoId
    """)
    suspend fun updateCompletionStatus(
        todoId: Long,
        isCompleted: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    // Convenience methods
    suspend fun markComplete(todoId: Long) =
        updateCompletionStatus(todoId, true)

    suspend fun markIncomplete(todoId: Long) =
        updateCompletionStatus(todoId, false)

}