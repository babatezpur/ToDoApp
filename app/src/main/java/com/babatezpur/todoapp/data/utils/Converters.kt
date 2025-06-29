package com.babatezpur.todoapp.data.utils

import androidx.room.TypeConverter
import com.babatezpur.todoapp.data.entities.Priority
import java.time.LocalDateTime
import java.time.ZoneOffset

class Converters {
    // Add any necessary converters here for Room or other data transformations
    // For example, if you need to convert LocalDateTime to Long for Room:
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime): Long = dateTime.toEpochSecond(ZoneOffset.UTC)

    @TypeConverter
    fun toLocalDateTime(epochSecond: Long): LocalDateTime = LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC)

    @TypeConverter
    fun fromPriority(priority: Priority): String {
        return priority.name  // Stores "P1", "P2", or "P3"
    }

    @TypeConverter
    fun toPriority(priorityString: String): Priority {
        return Priority.valueOf(priorityString)  // Converts back to enum
    }
}