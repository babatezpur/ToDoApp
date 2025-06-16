package com.babatezpur.todoapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.babatezpur.todoapp.data.entities.Todo
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Insert
    suspend fun insertTodo(todo: Todo)

    @Delete
    suspend fun deleteTodo(todo: Todo)

    @Delete
    suspend fun deleteTodos(todos: List<Todo>)

    @Update
    suspend fun updateTodo(todo: Todo)


    @Query("SELECT * FROM todos WHERE id = :todoId")
    suspend fun getTodoById(todoId: Long): Todo?

    // Primary method for the main To-Do list screen - shows current work
    @Query("SELECT * FROM todos WHERE is_completed = 0 ORDER BY created_at DESC")
    fun getActiveTodosByCreationDate(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE is_completed = 0 ORDER BY due_date ASC, priority ASC")
    fun getActiveTodosSortedByDueDate(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE due_date <= :currentDateTime AND is_completed = 0 ORDER BY due_date ASC")
    fun getOverdueTodos(currentDateTime: Long): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE is_completed = 0 ORDER BY due_date ASC")
    fun getActiveTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE is_completed = 1 ORDER BY due_date ASC")
    fun getCompletedTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE is_completed = 0 ORDER BY priority ASC, due_date ASC")
    fun getActiveTodosSortedByPriority(): Flow<List<Todo>>
}