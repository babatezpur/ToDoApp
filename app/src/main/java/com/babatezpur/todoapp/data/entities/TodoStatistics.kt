package com.babatezpur.todoapp.data.entities

// 📊 Data class for todo statistics
data class TodoStatistics(
    val totalTodos: Int,
    val completedTodos: Int,
    val activeTodos: Int,
    val completionRate: Int // Percentage
)