package com.babatezpur.todoapp.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L, // Use Long for auto-incrementing primary key in Room

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "priority")
    val priority: Priority,

    @ColumnInfo(name = "due_date")
    val dueDate: LocalDateTime,

    @ColumnInfo(name = "reminder_date_time")
    val reminderDateTime: LocalDateTime? = null, // NEW: Optional reminder

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean
)

enum class Priority { P1, P2, P3 } // Type safety is better than strings
