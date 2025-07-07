package com.babatezpur.todoapp.data.repositories

import com.babatezpur.todoapp.data.dao.TodoDao
import com.babatezpur.todoapp.data.entities.Priority
import com.babatezpur.todoapp.data.entities.Todo
import com.babatezpur.todoapp.domain.SortOption
import kotlinx.coroutines.flow.Flow



class TodoRepository (private val todoDao: TodoDao) {

    fun getAllTodos(): Flow<List<Todo>> = todoDao.getActiveTodos()

    fun searchActiveTodos(query: String): Flow<List<Todo>> =
        if(query.isBlank()) {
            todoDao.getActiveTodos()
        } else {
            todoDao.searchActiveTodosByTitle(query)
        }

    fun getTodoById(id: Long): Flow<Todo?> = todoDao.getTodoById(id)

    // 📋 DIRECT METHODS (for receivers - one-time fetch)
    suspend fun getTodoByIdDirect(id: Long): Todo? = todoDao.getTodoByIdDirect(id)

    suspend fun getAllTodosDirect(): List<Todo> = todoDao.getAllTodosDirect()

    suspend fun getAllActiveTodosDirect(): List<Todo> = todoDao.getAllActiveTodosDirect()


    suspend fun insertTodo(todo: Todo): Long = todoDao.insertTodo(todo)

    suspend fun updateTodo(todo: Todo) = todoDao.updateTodo(todo)

    suspend fun deleteTodo(todo: Todo) = todoDao.deleteTodo(todo)

    suspend fun markComplete(id: Long) = todoDao.markComplete(id)

    suspend fun markIncomplete(id: Long) = todoDao.markIncomplete(id)

    fun getCompletedTodos(): Flow<List<Todo>> = todoDao.getCompletedTodos()

    fun getTodosByPriority(): Flow<List<Todo>> =
        todoDao.getActiveTodosByPriority()

    fun getOverdueTodos(currentDateTime: Long): Flow<List<Todo>> = todoDao.getOverdueTodos()

    // 📋 SORT-ONLY METHODS (for backwards compatibility)
    fun getTodosSortedBy(sortOption: SortOption): Flow<List<Todo>> {
        return when (sortOption) {
            SortOption.CREATED_DESC -> todoDao.getActiveTodosSortedByCreatedDesc()
            SortOption.CREATED_ASC -> todoDao.getActiveTodosSortedByCreatedAsc()
            SortOption.PRIORITY -> todoDao.getActiveTodosSortedByPriority()
            SortOption.DUE_DATE_ASC -> todoDao.getActiveTodosSortedByDueDateAsc()
            SortOption.DUE_DATE_DESC -> todoDao.getActiveTodosSortedByDueDateDesc()
        }
    }

    fun getTodosWithSearchAndSort(
        query: String = "",
        sortOption: SortOption = SortOption.CREATED_DESC
    ) : Flow<List<Todo>> {
        return when {
            query.isBlank() -> {
                when (sortOption) {
                    SortOption.CREATED_DESC -> todoDao.getActiveTodosSortedByCreatedDesc()
                    SortOption.CREATED_ASC -> todoDao.getActiveTodosSortedByCreatedAsc()
                    SortOption.PRIORITY -> todoDao.getActiveTodosSortedByPriority()
                    SortOption.DUE_DATE_ASC -> todoDao.getActiveTodosSortedByDueDateAsc()
                    SortOption.DUE_DATE_DESC -> todoDao.getActiveTodosSortedByDueDateDesc()
                }
            }

            else -> {
                when (sortOption) {
                    SortOption.CREATED_DESC -> todoDao.searchActiveTodosByTitle(query)
                    SortOption.CREATED_ASC -> todoDao.searchActiveTodosByTitle(query)
                    SortOption.PRIORITY -> todoDao.searchActiveTodosByTitle(query)
                    SortOption.DUE_DATE_ASC -> todoDao.searchActiveTodosByTitle(query)
                    SortOption.DUE_DATE_DESC -> todoDao.searchActiveTodosByTitle(query)
                }
                // Note: For now, search always uses CREATED_DESC ordering
                // We can add the combined search+sort DAO methods later if needed
            }
        }
    }

    // 🗑️ BULK DELETE METHODS for Settings
    suspend fun deleteAllTodos() = todoDao.deleteAllTodos()

    suspend fun deleteAllCompletedTodos() = todoDao.deleteAllCompletedTodos()

    // 📊 STATISTICS METHODS for Settings
    suspend fun getTotalTodosCount(): Int = todoDao.getTotalTodosCount()

    suspend fun getCompletedTodosCount(): Int = todoDao.getCompletedTodosCount()

    suspend fun getActiveTodosCount(): Int = todoDao.getActiveTodosCount()

}