package com.babatezpur.todoapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.babatezpur.todoapp.domain.managers.TodoManager

class SettingsViewModelFactory(
    private val todoManager: TodoManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(todoManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}