package com.babatezpur.todoapp.ui.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.babatezpur.todoapp.R
import com.babatezpur.todoapp.data.database.TodoDatabase
import com.babatezpur.todoapp.data.entities.Todo
import com.babatezpur.todoapp.data.repositories.TodoRepository
import com.babatezpur.todoapp.domain.managers.TodoManager
import com.babatezpur.todoapp.ui.adapters.CompletedTodoAdapter
import com.babatezpur.todoapp.viewmodels.CompletedTodosUIState
import com.babatezpur.todoapp.viewmodels.CompletedTodosViewModel
import com.babatezpur.todoapp.viewmodels.CompletedTodosViewModelFactory
import kotlinx.coroutines.launch

class CompletedTodosActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var recyclerViewCompleted: RecyclerView
    private lateinit var completedTodoAdapter: CompletedTodoAdapter
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar

    private val completedTodoList = mutableListOf<Todo>()
    private lateinit var completedTodosViewModel: CompletedTodosViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_completed_todos)

        setupViewModel()
        setupViews()
        setupRecyclerView()
        setupClickListeners()
        oberveViewModel()

    }

    override fun onResume() {
        super.onResume()
        // Refresh completed todos when returning to this screen
        completedTodosViewModel.refreshTodos()
    }

    private fun setupViewModel() {
        val database = TodoDatabase.getDatabase(this)
        val repository = TodoRepository(database.todoDao())
        val todoManager = TodoManager(repository)

        val factory = CompletedTodosViewModelFactory(todoManager)
        completedTodosViewModel = ViewModelProvider(this, factory)[CompletedTodosViewModel::class.java]
    }

    private fun setupViews() {
        btnBack = findViewById(R.id.btnBack)
        recyclerViewCompleted = findViewById(R.id.recyclerViewCompleted)
        emptyView = findViewById(R.id.completed_empty_view)
        progressBar = findViewById(R.id.progressBar)

        // Initially hide empty view
        emptyView.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        completedTodoAdapter = CompletedTodoAdapter(completedTodoList,
            onTodoClick = { _, _ ->
                // Do nothing on click for completed todos
            },
            onTodoUncheck = { todo, position ->
                handleTodoUncheck(todo, position)
            }
        )
        recyclerViewCompleted.apply {
            adapter = completedTodoAdapter
            layoutManager = LinearLayoutManager(this@CompletedTodosActivity)
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun oberveViewModel() {
        lifecycleScope.launch{
            completedTodosViewModel.uiState.collect { state ->
                handleUiState(state)
            }
        }
    }

    private fun handleUiState(state: CompletedTodosUIState) {
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        state.error?.let { error ->
            Log.e("CompletedTodos", "Error: $error")
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            completedTodosViewModel.clearError()
        }

        // Handle success messages
        state.successMessage?.let { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            completedTodosViewModel.clearSuccessMessage()
        }

        // Update completed todos list
        updateCompletedTodosList(state.completedTodos)

    }

    private fun updateCompletedTodosList(todos: List<Todo>) {
        completedTodoList.clear()
        completedTodoList.addAll(todos)
        completedTodoAdapter.notifyDataSetChanged()

        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (completedTodoList.isEmpty()) {
            recyclerViewCompleted.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerViewCompleted.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }

    private fun handleTodoUncheck(todo: Todo, position: Int) {
        Log.d("CompletedTodos", "Todo unchecked: ${todo.title}")

        val viewHolder = recyclerViewCompleted.findViewHolderForAdapterPosition(position)

        if (viewHolder != null) {
            // Animate the removal of the todo item
            animateItemSlideOut(viewHolder.itemView, todo, position)
        } else {
            Toast.makeText(this, "Moving '${todo.title}' back to active todos...", Toast.LENGTH_SHORT).show()
            completedTodosViewModel.markTodoIncomplete(todo)
        }
    }

    private fun animateItemSlideOut(itemView: View, todo: Todo, position: Int) {

        Toast.makeText(this, "Moving '${todo.title}' back to active todos...", Toast.LENGTH_SHORT).show()

        // Disable click during animation to prevent multiple triggers
        itemView.isClickable = false

        // Get screen width for slide distance
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()

        itemView.animate()
            .translationX(screenWidth)
            .alpha(0.3f)
            .setDuration(1000)
            .withEndAction {
                // Remove the item from the adapter after animation
                completedTodosViewModel.markTodoIncomplete(todo)

                // Reset the view properties for when it gets recycled
                itemView.translationX = 0f
                itemView.alpha = 1.0f
                itemView.isClickable = true
            }
            .start()
    }


}