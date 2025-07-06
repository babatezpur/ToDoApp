package com.babatezpur.todoapp.viewmodels

import androidx.lifecycle.ViewModelProvider
import com.babatezpur.todoapp.domain.managers.TodoManager

class CompletedTodosViewModelFactory(private val todoManager: TodoManager)
    : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CompletedTodosViewModel::class.java)) {
            return CompletedTodosViewModel(todoManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}