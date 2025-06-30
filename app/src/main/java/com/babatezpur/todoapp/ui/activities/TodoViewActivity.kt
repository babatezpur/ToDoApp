package com.babatezpur.todoapp.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.babatezpur.todoapp.R
import com.babatezpur.todoapp.data.database.TodoDatabase
import com.babatezpur.todoapp.data.entities.Priority
import com.babatezpur.todoapp.data.entities.Todo
import com.babatezpur.todoapp.data.repositories.TodoRepository
import com.babatezpur.todoapp.domain.SortOption
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
    private val todoList = mutableListOf<Todo>()
    private var selectedTodo: Todo? = null
    private lateinit var fabAddTodo : FloatingActionButton
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    // Initialize the ViewModel if needed
    private lateinit var todoViewViewModel: TodoViewViewModel

    // 🔍 NEW: Toolbar views
    private lateinit var toolbarTitle: TextView
    private lateinit var searchButton: ImageButton
    private lateinit var sortButton: ImageButton
    private lateinit var moreOptionsButton: ImageButton

    // 🔍 NEW: Search views (we'll add these dynamically)
    private lateinit var searchEditText: EditText
    private lateinit var closeSearchButton: ImageButton



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_view)


        setupViewModel()
        setupViews()
        setupToolbar()
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

    private fun setupToolbar() {
        toolbarTitle = findViewById(R.id.textViewTitle)
        searchButton = findViewById(R.id.searchButton)
        sortButton = findViewById(R.id.sortButton)
        moreOptionsButton = findViewById(R.id.moreOptionsButton)

        // Find search views
        searchEditText = findViewById(R.id.searchEditText)
        closeSearchButton = findViewById(R.id.closeSearchButton)

        // Set up search button click listener
        searchButton.setOnClickListener {
            Log.d("TodoView", "Search button clicked")
            handleSearchButtonClick()
        }

        // Set up sort button click listener
        sortButton.setOnClickListener {
            Log.d("TodoView", "Sort button clicked")
            handleSortButtonClick()
        }
        closeSearchButton.setOnClickListener {
            todoViewViewModel.deactivateSearch()
        }


        // Set up more options button click listener
        moreOptionsButton.setOnClickListener {
            Log.d("TodoView", "More options button clicked")
            handleMoreOptionsClick()
        }

        setupSearchEditText()
    }

    private var searchRunnable: Runnable? = null

    private fun setupSearchEditText() {
        searchEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                Log.d("TodoView", "Search query changed: '$query'")

                // Only update if we're in search mode to avoid issues
                if (todoViewViewModel.uiState.value.isSearchActive) {
                    // Cancel previous search if user is still typing
                    searchRunnable?.let { handler.removeCallbacks(it) }
                    
                    // Create new search with 300ms delay
                    searchRunnable = Runnable {
                        todoViewViewModel.updateSearchQuery(query)
                    }
                    handler.postDelayed(searchRunnable!!, 300)
                }
            }
        })
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

        // 🔍 NEW: Handle search state
        if (state.isSearchActive) {
            showSearchView()
        } else {
            hideSearchView()
        }


        updateToolbarTitle()

        // Update todo list
        updateTodoList(state.todos)

        // 🔍 NEW: Update empty view message based on search
        updateEmptyViewMessage(state)
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

    private fun handleSearchButtonClick() {
        Log.d("TodoView", "Search button clicked")

        val currentState = todoViewViewModel.uiState.value
        if (currentState.isSearchActive) {
            // Search is active, close it
            todoViewViewModel.deactivateSearch()
        } else {
            // Activate search mode
            todoViewViewModel.activateSearch()
        }
    }

    private fun handleSortButtonClick() {
        Log.d("TodoView", "Sort button clicked")
        showSortDialog()
    }

    private fun showSortDialog() {
        val currentState = todoViewViewModel.uiState.value
        val sortOptions = currentState.availableSortOptions
        val currentSort = currentState.currentSortOption

        // Create display names array
        val displayNames = sortOptions.map { option ->
            todoViewViewModel.getSortDisplayName(option)
        }.toTypedArray()

        // Find current selection index
        val currentIndex = sortOptions.indexOf(currentSort)

        AlertDialog.Builder(this)
            .setTitle("Sort todos by")
            .setSingleChoiceItems(displayNames, currentIndex) { dialog, which ->
                val selectedOption = sortOptions[which]
                todoViewViewModel.updateSortOption(selectedOption)
                dialog.dismiss()

                // Update toolbar title to show current sort
                updateToolbarTitle()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSearchView() {
        Log.d("TodoView", "Showing search view")

        // Hide normal toolbar elements
        toolbarTitle.visibility = View.GONE
        searchButton.visibility = View.GONE

        // Show search elements
        searchEditText.visibility = View.VISIBLE
        closeSearchButton.visibility = View.VISIBLE

        // Set text to current search query from ViewModel
        val currentQuery = todoViewViewModel.uiState.value.searchQuery
        searchEditText.setText(currentQuery)

        // Use post to ensure layout is complete before focusing
        searchEditText.post {
            // Ensure EditText is focusable and enabled
            searchEditText.isFocusable = true
            searchEditText.isFocusableInTouchMode = true
            searchEditText.isEnabled = true
            
            searchEditText.requestFocus()
            
            // Set cursor to end of text
            searchEditText.setSelection(searchEditText.text.length)

            // Force show keyboard
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(searchEditText, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
        }
    }

    // 🔍 NEW: Hide search view and show normal toolbar
    private fun hideSearchView() {
        Log.d("TodoView", "Hiding search view")

        // Hide keyboard first
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText?.windowToken, 0)

        // Clear focus
        searchEditText.clearFocus()

        // Hide search elements
        searchEditText.visibility = View.GONE
        closeSearchButton.visibility = View.GONE

        // Show normal toolbar elements
        toolbarTitle.visibility = View.VISIBLE
        searchButton.visibility = View.VISIBLE
    }



    // NEW: Handle more options (placeholder for future features)
    private fun handleMoreOptionsClick() {
        Log.d("TodoView", "More options clicked")
        // TODO: Implement menu with options like "Delete completed", "Export", etc.
        Toast.makeText(this, "More options - Coming soon!", Toast.LENGTH_SHORT).show()
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
        Log.d("TodoDebug", "Todo completion changed: ${todo.title} -> $isChecked")

        when {
            isChecked && !todo.isCompleted -> {
                // ✅ Completing a todo - show visual feedback with delay
                handleTodoCompletionWithDelay(todo, position)
            }
            !isChecked && todo.isCompleted -> {
                // ✅ Unchecking a completed todo
                todoViewViewModel.toggleTodoCompletion(todo)
                Toast.makeText(this, "Todo marked as incomplete", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // No change in state, revert checkbox
                todoAdapter.notifyItemChanged(position)
            }
        }
    }

    private fun handleTodoCompletionWithDelay(todo: Todo, position: Int) {
        // The checkbox is already visually checked due to user interaction
        // Now we handle the backend update with delay

        // Disable the checkbox temporarily to prevent multiple clicks
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? TodoAdapter.TodoViewHolder
        viewHolder?.checkBoxComplete?.isEnabled = false

        // Show immediate feedback
        Toast.makeText(this, "Completing todo...", Toast.LENGTH_SHORT).show()

        // Call ViewModel method that handles the delay and database update
        todoViewViewModel.markTodoCompleteWithDelay(todo)

        // Re-enable checkbox after a short delay (in case of error)
        recyclerView.postDelayed({
            viewHolder?.checkBoxComplete?.isEnabled = true
        }, 2000)
    }

    private fun updateToolbarTitle() {
        val currentState = todoViewViewModel.uiState.value

        when {
            currentState.isSearchActive -> {
                // Search mode - title will be hidden anyway
                return
            }
            currentState.currentSortOption != SortOption.CREATED_DESC -> {
                // Show current sort in title
                val sortName = todoViewViewModel.getCurrentSortDisplayName()
                toolbarTitle.text = "Todos - $sortName"
            }
            else -> {
                // Default title
                toolbarTitle.text = "My Todos"
            }
        }
    }

    private fun updateEmptyViewMessage(state: TodoViewUiState) {
        if (todoList.isEmpty()) {
            recyclerView.visibility = RecyclerView.GONE
            emptyView.visibility = TextView.VISIBLE

            // Different messages based on state
            emptyView.text = when {
                state.isSearchActive && state.searchQuery.isNotBlank() -> {
                    "No todos found for \"${state.searchQuery}\""
                }
                state.currentSortOption != SortOption.CREATED_DESC -> {
                    "No todos to show with current sorting"
                }
                else -> {
                    "No todos yet!\nTap + to add your first todo"
                }
            }
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

    // 🔧 NEW: Override back button to handle search state
    override fun onBackPressed() {
        val currentState = todoViewViewModel.uiState.value
        if (currentState.isSearchActive) {
            // Close search instead of closing activity
            todoViewViewModel.deactivateSearch()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        // Clean up search debounce handler
        searchRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }

}