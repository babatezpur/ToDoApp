package com.babatezpur.todoapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.babatezpur.todoapp.data.dao.TodoDao
import com.babatezpur.todoapp.data.entities.Todo
import com.babatezpur.todoapp.data.utils.Converters
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized

@Database(
    entities = [Todo::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao

    companion object{
        private var INSTANCE: TodoDatabase? = null

        @OptIn(InternalCoroutinesApi::class)
        fun getDatabase(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder<TodoDatabase>(
                    context = context.applicationContext,
                    name = "todo_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}