package com.babatezpur.todoapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.babatezpur.todoapp.domain.managers.TodoManager

class AddTodoViewModelFactory(
    private val todoManager : TodoManager,
    private val context: android.content.Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddTodoViewModel::class.java)) {
            return AddTodoViewModel(todoManager, context ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}