package com.babatezpur.todoapp.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.babatezpur.todoapp.R
import com.babatezpur.todoapp.data.entities.Priority
import com.babatezpur.todoapp.data.entities.Todo
import com.babatezpur.todoapp.ui.adapters.TodoAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.LocalDateTime

class TodoViewActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var todoAdapter: TodoAdapter
    private val todoList = mutableListOf<Todo>() // Assuming Todo is a data class representing a to-do item
    private var selectedTodo: Todo? = null
    private lateinit var fabAddTodo : FloatingActionButton
    private lateinit var emptyView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_view)

        setupRecyclerView()
        setupFAB()
        loadTodos()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.todo_empty_view)
        Log.d("TodoDebug", "Setting up RecyclerView with ${todoList.size} items")
        todoAdapter = TodoAdapter(
            todoList,
            onTodoClick = { todo, pos ->
                // Handle todo item click
                // handleTodoClick(todo, pos)
            },
            onTodoComplete = { todo, pos, isChecked ->
                // Handle todo completion
               // handleTodoCompletion(todo, pos, isChecked)
            }
        )
        recyclerView.apply {
            adapter = todoAdapter
            layoutManager = LinearLayoutManager(this@TodoViewActivity)
            setHasFixedSize(true)
        }
    }

    private fun setupFAB() {
        fabAddTodo = findViewById(R.id.fab_add_todo)
        fabAddTodo.setOnClickListener {
            // Handle FAB click to add a new todo
            val intent = Intent(this, AddTodoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadTodos() {
        // Add sample data for testing
        addSampleTodos()
        updateEmptyState()
    }


    private fun updateEmptyState() {
        if (todoList.isEmpty()) {
            recyclerView.visibility = RecyclerView.GONE
            emptyView.visibility = TextView.VISIBLE
        } else {
            recyclerView.visibility = RecyclerView.VISIBLE
            emptyView.visibility = TextView.GONE
        }
    }

    private fun removeTodoFromList(completedTodo: Todo) {
        val position = todoList.indexOf(completedTodo)
        if (position != -1) {
            todoList.removeAt(position)
            todoAdapter.notifyItemRemoved(position)
            updateEmptyState() // Add this line
//            showUndoSnackbar(completedTodo, position)
        }
    }

    private fun addSampleTodos() {
        val sampleTodos = listOf(
            Todo(
                id = 0L, // Will auto-generate
                title = "Submit quarterly report",
                description = "Compile Q2 financial data and submit to management by end of week",
                dueDate = LocalDateTime.of(2025, 6, 20, 17, 0), // June 20, 2025 at 5:00 PM
                priority = Priority.P1, // High priority
                isCompleted = false
            ),
            Todo(
                id = 0L,
                title = "Buy groceries",
                description = "Get milk, bread, eggs, chicken, vegetables, and fruits from supermarket",
                dueDate = LocalDateTime.of(2025, 6, 19, 18, 30), // June 19, 2025 at 6:30 PM
                priority = Priority.P2, // Medium priority
                isCompleted = false
            ),
            Todo(
                id = 0L,
                title = "Schedule dentist appointment",
                description = "Call dental office to book annual cleaning and checkup",
                dueDate = LocalDateTime.of(2025, 6, 25, 10, 0), // June 25, 2025 at 10:00 AM
                priority = Priority.P3, // Low priority
                isCompleted = false
            ),
            Todo(
                id = 0L,
                title = "Prepare presentation",
                description = "Create slides for Monday's client meeting on new product features",
                dueDate = LocalDateTime.of(2025, 6, 21, 9, 0), // June 21, 2025 at 9:00 AM
                priority = Priority.P1, // High priority
                isCompleted = false
            ),
            Todo(
                id = 0L,
                title = "Water house plants",
                description = "Check all indoor plants and water as needed, especially the peace lily",
                dueDate = LocalDateTime.of(2025, 6, 18, 8, 0), // June 18, 2025 at 8:00 AM
                priority = Priority.P3, // Low priority
                isCompleted = false
            ),
            Todo(
                id = 0L,
                title = "Book flight tickets",
                description = "Find and book round-trip tickets to New York for vacation next month",
                dueDate = LocalDateTime.of(2025, 6, 23, 14, 0), // June 23, 2025 at 2:00 PM
                priority = Priority.P2, // Medium priority
                isCompleted = false
            )
        )

        todoList.addAll(sampleTodos)
        Log.d("TodoDebug", "Added ${todoList.size} todos to list")
        todoAdapter.notifyDataSetChanged()
    }



}