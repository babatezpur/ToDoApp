package com.babatezpur.todoapp.viewmodels


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.babatezpur.todoapp.domain.managers.TodoManager

class EditTodoViewModelFactory(
    private val todoManager: TodoManager,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditTodoViewModel::class.java)) {
            return EditTodoViewModel(todoManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}