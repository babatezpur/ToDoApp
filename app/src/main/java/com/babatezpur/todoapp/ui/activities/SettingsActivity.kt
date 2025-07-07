package com.babatezpur.todoapp.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.babatezpur.todoapp.R
import com.babatezpur.todoapp.data.database.TodoDatabase
import com.babatezpur.todoapp.data.entities.TodoStatistics
import com.babatezpur.todoapp.data.repositories.TodoRepository
import com.babatezpur.todoapp.domain.managers.TodoManager
import com.babatezpur.todoapp.viewmodels.SettingsUiState
import com.babatezpur.todoapp.viewmodels.SettingsViewModel
import com.babatezpur.todoapp.viewmodels.SettingsViewModelFactory
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    // UI Components
    private lateinit var btnBack: ImageButton
    private lateinit var btnClearAllTodos: Button
    private lateinit var btnClearCompletedTodos: Button
    private lateinit var progressBar: ProgressBar

    // Statistics TextViews
    private lateinit var tvTotalTodos: TextView
    private lateinit var tvActiveTodos: TextView
    private lateinit var tvCompletedTodos: TextView
    private lateinit var tvCompletionRate: TextView

    // ViewModel
    private lateinit var settingsViewModel: SettingsViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupViewModel()
        setupViews()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh statistics when returning to settings
        settingsViewModel.loadStatistics()
    }

    private fun setupViewModel() {
        val database = TodoDatabase.getDatabase(this)
        val repository = TodoRepository(database.todoDao())
        val todoManager = TodoManager(repository)

        val factory = SettingsViewModelFactory(todoManager)
        settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
    }

    private fun setupViews() {
        btnBack = findViewById(R.id.btnBack)
        btnClearAllTodos = findViewById(R.id.btnClearAllTodos)
        btnClearCompletedTodos = findViewById(R.id.btnClearCompletedTodos)
        progressBar = findViewById(R.id.progressBar)

        // Statistics TextViews
        tvTotalTodos = findViewById(R.id.tvTotalTodos)
        tvActiveTodos = findViewById(R.id.tvActiveTodos)
        tvCompletedTodos = findViewById(R.id.tvCompletedTodos)
        tvCompletionRate = findViewById(R.id.tvCompletionRate)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnClearAllTodos.setOnClickListener {
            showClearAllTodosDialog()
        }

        btnClearCompletedTodos.setOnClickListener {
            showClearCompletedTodosDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            settingsViewModel.uiState.collect { state ->
                handleUiState(state)
            }
        }
    }

    private fun handleUiState(state: SettingsUiState) {
        // Show or hide progress bar
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        btnClearAllTodos.isEnabled = !state.isLoading
        btnClearCompletedTodos.isEnabled = !state.isLoading

        state.successMessage?.let { message ->
            // Show success message (e.g., using a Snackbar or Toast)
            // For simplicity, using Toast here
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            settingsViewModel.clearMessages() // Clear after showing
        }

        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            settingsViewModel.clearMessages()
        }

        // Update statistics
        state.statistics?.let { stats ->
            updateStatisticsDisplay(stats)
        }
    }

    private fun updateStatisticsDisplay(stats: TodoStatistics) {
        tvTotalTodos.text = "Total Todos: ${stats.totalTodos}"
        tvActiveTodos.text = "Active Todos: ${stats.activeTodos}"
        tvCompletedTodos.text = "Completed Todos: ${stats.completedTodos}"
        tvCompletionRate.text = "Completion Rate: ${stats.completionRate}%"

        // Update button states based on data
        btnClearAllTodos.isEnabled = stats.totalTodos > 0
        btnClearCompletedTodos.isEnabled = stats.completedTodos > 0

        // Update button text to show counts
        btnClearAllTodos.text = if (stats.totalTodos > 0) {
            "Clear All Todos (${stats.totalTodos})"
        } else {
            "Clear All Todos"
        }

        btnClearCompletedTodos.text = if (stats.completedTodos > 0) {
            "Clear Completed Todos (${stats.completedTodos})"
        } else {
            "Clear Completed Todos"
        }
    }

    private fun showClearAllTodosDialog() {
        val stats = settingsViewModel.uiState.value.statistics
        val todoCount = stats?.totalTodos ?: 0

        if (todoCount == 0) {
            Toast.makeText(this, "No todos to clear!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Clear All Todos")
            .setMessage("Are you sure you want to delete all $todoCount todos?\n\nThis will delete both active and completed todos.\n\nThis action cannot be undone.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Delete All") { _, _ ->
                settingsViewModel.deleteAllTodos()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * 🗑️ Show confirmation dialog for clearing completed todos
     */
    private fun showClearCompletedTodosDialog() {
        val stats = settingsViewModel.uiState.value.statistics
        val completedCount = stats?.completedTodos ?: 0

        if (completedCount == 0) {
            Toast.makeText(this, "No completed todos to clear!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Clear Completed Todos")
            .setMessage("Are you sure you want to delete all $completedCount completed todos?\n\nThis action cannot be undone.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Delete Completed") { _, _ ->
                settingsViewModel.deleteAllCompletedTodos()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

