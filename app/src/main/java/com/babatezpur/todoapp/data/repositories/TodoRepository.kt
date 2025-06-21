package com.babatezpur.todoapp.data.repositories

import com.babatezpur.todoapp.data.dao.TodoDao
import com.babatezpur.todoapp.data.entities.Priority
import com.babatezpur.todoapp.data.entities.Todo
import kotlinx.coroutines.flow.Flow

class TodoRepository (private val todoDao: TodoDao) {

    fun getAllTodos(): Flow<List<Todo>> = todoDao.getActiveTodos()

    fun getTodoById(id: Long): Flow<Todo?> = todoDao.getTodoById(id)

    suspend fun insertTodo(todo: Todo): Long = todoDao.insertTodo(todo)

    suspend fun updateTodo(todo: Todo) = todoDao.updateTodo(todo)

    suspend fun deleteTodo(todo: Todo) = todoDao.deleteTodo(todo)

    suspend fun markComplete(id: Long) = todoDao.markComplete(id)

    suspend fun markIncomplete(id: Long) = todoDao.markIncomplete(id)

    fun getTodosByPriority(): Flow<List<Todo>> =
        todoDao.getActiveTodosByPriority()

    fun getOverdueTodos(currentDateTime: Long): Flow<List<Todo>> = todoDao.getOverdueTodos(currentDateTime)

}