package com.babatezpur.todoapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.babatezpur.todoapp.domain.managers.TodoManager

class TodoViewViewModelFactory(
    private val todoManager: TodoManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewViewModel::class.java)) {
            return TodoViewViewModel(todoManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}