package com.babatezpur.todoapp.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.babatezpur.todoapp.R
import com.babatezpur.todoapp.data.database.TodoDatabase
import com.babatezpur.todoapp.data.entities.Priority
import com.babatezpur.todoapp.data.entities.Todo
import com.babatezpur.todoapp.data.repositories.TodoRepository
import com.babatezpur.todoapp.domain.managers.TodoManager
import com.babatezpur.todoapp.ui.adapters.TodoAdapter
import com.babatezpur.todoapp.viewmodels.TodoViewUiState
import com.babatezpur.todoapp.viewmodels.TodoViewViewModel
import com.babatezpur.todoapp.viewmodels.TodoViewViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TodoViewActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var todoAdapter: TodoAdapter
    private val todoList = mutableListOf<Todo>() // Assuming Todo is a data class representing a to-do item
    private var selectedTodo: Todo? = null
    private lateinit var fabAddTodo : FloatingActionButton
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar

    // Initialize the ViewModel if needed
    private lateinit var todoViewViewModel: TodoViewViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_view)

        setupViewModel()
        setupViews()
        setupRecyclerView()
        setupFAB()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh todos when returning from AddTodoActivity
        todoViewViewModel.refreshTodos()
    }

    private fun setupViewModel() {
        // Create dependencies
        val database = TodoDatabase.getDatabase(this)
        val repository = TodoRepository(database.todoDao())
        val todoManager = TodoManager(repository)

        // Create ViewModel with factory
        val factory = TodoViewViewModelFactory(todoManager)
        todoViewViewModel = ViewModelProvider(this, factory)[TodoViewViewModel::class.java]
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.todo_empty_view)
        progressBar = findViewById(R.id.progressBar)
        fabAddTodo = findViewById(R.id.fab_add_todo)
        emptyView.visibility = TextView.GONE // Initially hidden
    }


    private fun setupRecyclerView() {
        emptyView = findViewById(R.id.todo_empty_view)
        Log.d("TodoDebug", "Setting up RecyclerView with ${todoList.size} items")
        todoAdapter = TodoAdapter(
            todoList,
            onTodoClick = { todo, position ->
                handleTodoClick(todo, position)
            },
            onTodoComplete = { todo, position, isChecked ->
                handleTodoCompletion(todo, position, isChecked)
            }
        )
        recyclerView.apply {
            adapter = todoAdapter
            layoutManager = LinearLayoutManager(this@TodoViewActivity)
            setHasFixedSize(true)
        }
    }

    private fun setupFAB() {
        Log.d("TodoView", "setupFAB called")
        // ... your existing logs ...

        fabAddTodo.setOnTouchListener { v, event ->
            Log.d("TodoView", "FAB touch detected: ${event.action}")
            false // Don't consume the event
        }

        fabAddTodo.setOnClickListener {
            Log.d("TodoView", "FAB clicked to add new todo")
            val intent = Intent(this, AddTodoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            todoViewViewModel.uiState.collect { state ->
                handleUiState(state)
            }
        }
    }

    private fun handleUiState(state: TodoViewUiState) {
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        state.error?.let {
            if (it.isNotEmpty()) {
                Log.e("TodoDebug", "Error loading todos: $it")
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                // Show error message to user
            }
        }
        updateTodoList(state.todos)
    }

    private fun updateTodoList(todos: List<Todo>) {
        todoList.clear()
        todoList.addAll(todos)
        todoAdapter.notifyDataSetChanged()

        updateEmptyState() // Update empty state visibility
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

    private fun handleTodoClick(todo: Todo, position: Int) {
        selectedTodo = todo
        // Handle the click event, e.g., open a detail view or edit screen
        val intent = Intent(this, EditTodoActivity::class.java).apply {
            putExtra("todo_id", todo.id)
        }
        startActivity(intent)
    }

    private fun handleTodoCompletion(todo: Todo, position: Int, isChecked: Boolean) {
        if (isChecked) {
            // Mark todo as completed
            todoViewViewModel.markTodoComplete(todo)
            //removeTodoFromList(todo) // Remove from list after marking complete
        } else {
            // Handle unchecking if needed
            Toast.makeText(this, "Todo marked as incomplete", Toast.LENGTH_SHORT).show()
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





}