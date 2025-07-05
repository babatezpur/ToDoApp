package com.babatezpur.todoapp.ui.activities

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.babatezpur.todoapp.R
import com.babatezpur.todoapp.data.database.TodoDatabase
import com.babatezpur.todoapp.data.entities.Priority
import com.babatezpur.todoapp.data.entities.Todo
import com.babatezpur.todoapp.data.repositories.TodoRepository
import com.babatezpur.todoapp.domain.managers.TodoManager
import com.babatezpur.todoapp.viewmodels.EditTodoUiState
import com.babatezpur.todoapp.viewmodels.EditTodoViewModel
import com.babatezpur.todoapp.viewmodels.EditTodoViewModelFactory
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class EditTodoActivity : AppCompatActivity() {

    // UI Views
    private lateinit var btnBack: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var tilTitle: TextInputLayout
    private lateinit var rgPriority: RadioGroup
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvSelectedTime: TextView
    private lateinit var switchReminder: Switch
    private lateinit var layoutReminderDetails: LinearLayout
    private lateinit var tvReminderDate: TextView
    private lateinit var tvReminderTime: TextView
    private lateinit var btnUpdateTodo: Button

    // ViewModel
    private lateinit var editTodoViewModel: EditTodoViewModel

    // Data
    private var todoId: Long = -1L
    private var currentTodo: Todo? = null
    private var dueDate: LocalDate? = null
    private var dueTime: LocalTime? = null
    private var reminderDate: LocalDate? = null
    private var reminderTime: LocalTime? = null

    // Formatters
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    // Track if we're currently loading data (to avoid marking as unsaved changes)
    private var isLoadingData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_todo)

        // Get todo ID from intent
        todoId = intent.getLongExtra("todo_id", -1L)
        if (todoId == -1L) {
            Toast.makeText(this, "Invalid todo ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViewModel()
        initViews()
        setupClickListeners()
        setupTextWatchers()
        observeViewModel()

        // Load the todo
        editTodoViewModel.loadTodo(todoId)
    }

    private fun setupViewModel() {
        val database = TodoDatabase.getDatabase(this)
        val repository = TodoRepository(database.todoDao())
        val todoManager = TodoManager(repository)
        val factory = EditTodoViewModelFactory(
            todoManager = todoManager,
            context = this
        )
        editTodoViewModel = ViewModelProvider(this, factory)[EditTodoViewModel::class.java]
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnDelete = findViewById(R.id.btnDelete)
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        tilTitle = findViewById(R.id.tilTitle)
        rgPriority = findViewById(R.id.rgPriority)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvSelectedTime = findViewById(R.id.tvSelectedTime)
        switchReminder = findViewById(R.id.switchReminder)
        layoutReminderDetails = findViewById(R.id.layoutReminderDetails)
        tvReminderDate = findViewById(R.id.tvReminderDate)
        tvReminderTime = findViewById(R.id.tvReminderTime)
        btnUpdateTodo = findViewById(R.id.btnUpdateTodo)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { handleBackPress() }
        btnDelete.setOnClickListener { showDeleteConfirmationDialog() }
        tvSelectedDate.setOnClickListener {
            showDatePicker(isForReminder = false)
        }

        tvSelectedTime.setOnClickListener {
            showTimePicker(isForReminder = false)
        }
        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                layoutReminderDetails.visibility = View.VISIBLE
                if (!isLoadingData) {
                    Toast.makeText(this, "Please set reminder date and time", Toast.LENGTH_SHORT).show()
                    checkAndRequestPermissions()
                }
            } else {
                layoutReminderDetails.visibility = View.GONE
                if (!isLoadingData) {
                    reminderDate = null
                    reminderTime = null
                    resetReminderViews()
                    checkForUnsavedChanges()
                }
            }
        }

        tvReminderDate.setOnClickListener {
            showDatePicker(isForReminder = true)
        }
        tvReminderTime.setOnClickListener {
            showTimePicker(isForReminder = true)
        }
        btnUpdateTodo.setOnClickListener {
            updateTodo()
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (!isLoadingData) {
                    checkForUnsavedChanges()
                }
            }
        }

        etTitle.addTextChangedListener(textWatcher)

        etDescription.addTextChangedListener(textWatcher)

        rgPriority.setOnCheckedChangeListener { _, _ ->
            if (!isLoadingData) {
                checkForUnsavedChanges()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            editTodoViewModel.uiState.collect { state ->
                handleUiState(state)
            }
        }
    }

    private fun handleUiState(state: EditTodoUiState) {
        if (state.isLoading){
            btnUpdateTodo.isEnabled = false
            btnUpdateTodo.text = if (state.todo == null) "Loading..." else "Updating..."
            btnDelete.isEnabled = false
        } else {
            btnUpdateTodo.isEnabled = true
            btnDelete.isEnabled = true
            btnUpdateTodo.text = "Update Todo"
        }

        state.todo?.let { todo ->
            if (currentTodo == null) {
                currentTodo = todo
                populateFields(todo)
            }
        }

        if (state.isUpdateSuccess) {
            Toast.makeText(this, "Todo updated successfully", Toast.LENGTH_SHORT).show()
            editTodoViewModel.resetSuccessStates()
            finish()
        }

        if (state.isDeleteSuccess) {
            Toast.makeText(this, "Todo deleted successfully", Toast.LENGTH_SHORT).show()
            editTodoViewModel.resetSuccessStates()
            finish()
        }

        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            editTodoViewModel.clearError()
        }
    }

    private fun populateFields(todo: Todo) {
        isLoadingData = true
        etTitle.setText(todo.title)
        etDescription.setText(todo.description)
        tilTitle.error = null

        when (todo.priority) {
            Priority.P1 -> rgPriority.check(R.id.rbHighPriority)
            Priority.P2 -> rgPriority.check(R.id.rbMediumPriority)
            Priority.P3 -> rgPriority.check(R.id.rbLowPriority)
        }

        dueDate = todo.dueDate.toLocalDate()
        dueTime = todo.dueDate.toLocalTime()
        tvSelectedDate.text = dueDate?.format(dateFormatter)
        tvSelectedTime.text = dueTime?.format(timeFormatter)


        if (todo.reminderDateTime != null) {
            switchReminder.isChecked = true
            layoutReminderDetails.visibility = View.VISIBLE
            reminderDate = todo.reminderDateTime.toLocalDate()
            reminderTime = todo.reminderDateTime.toLocalTime()
            tvReminderDate.text = reminderDate?.format(dateFormatter)
            tvReminderTime.text = reminderTime?.format(timeFormatter)
        } else {
            switchReminder.isChecked = false
            layoutReminderDetails.visibility = View.GONE
            resetReminderViews()
        }

        isLoadingData = false
    }

    private fun updateTodo() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val priority = when (rgPriority.checkedRadioButtonId) {
            R.id.rbHighPriority -> "HIGH"
            R.id.rbMediumPriority -> "MEDIUM"
            R.id.rbLowPriority -> "LOW"
            else -> "MEDIUM" // Default to Medium if none selected
        }

        // Validation
        if (title.isEmpty()) {
            tilTitle.error = "Title cannot be empty"
            etTitle.requestFocus()
            return
        } else {
            tilTitle.error = null
        }

        if (dueDate == null || dueTime == null) {
            Toast.makeText(this, "Please select a due date and time", Toast.LENGTH_SHORT).show()
            return
        }

        if (dueTime == null) {
            Toast.makeText(this, "Please select a due time", Toast.LENGTH_SHORT).show()
            return
        }

        val dueDateTime = LocalDateTime.of(
            dueDate!!,
            dueTime!!
        )

        // Validate due date
        val dueDateError = editTodoViewModel.validateDueDateTime(dueDateTime)
        if (dueDateError != null) {
            Toast.makeText(this, "❌ $dueDateError", Toast.LENGTH_SHORT).show()
            return
        }

        val reminderDateTime = if(switchReminder.isChecked && reminderDate != null && reminderTime != null) {
            val reminder = LocalDateTime.of(
                reminderDate!!,
                reminderTime!!
            )

            val reminderError = editTodoViewModel.validateReminderTime(reminder)
            if (reminderError != null) {
                Toast.makeText(this, "❌ $reminderError", Toast.LENGTH_SHORT).show()
                return
            }

            val beforeDueError = editTodoViewModel.validateReminderBeforeDue(reminder, dueDateTime)
            if (beforeDueError != null) {
                Toast.makeText(this, "❌ $beforeDueError", Toast.LENGTH_SHORT).show()
                return
            }

            reminder
        } else {
            null
        }

        // Update the todo
        editTodoViewModel.updateTodo(
            title = title,
            description = description,
            priority = priority,
            dueDateTime = dueDateTime,
            reminderDateTime = reminderDateTime
        )
    }

    private fun showDeleteConfirmationDialog() {
        // Show confirmation dialog before deleting
        AlertDialog.Builder(this)
            .setTitle("Delete Todo")
            .setMessage("Are you sure you want to delete this todo?")
            .setPositiveButton("Delete") { _, _ ->
                editTodoViewModel.deleteTodo()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleBackPress() {
        val hasUnsavedChanges = editTodoViewModel.uiState.value.hasUnsavedChanges
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog()
        } else {
            finish()
        }
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. What would you like to do?")
            .setPositiveButton("Save") { _, _ ->
                updateTodo()
            }
            .setNegativeButton("Discard") { _, _ ->
                finish()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun checkForUnsavedChanges() {
        if (currentTodo == null || isLoadingData) return;

        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val priority = when (rgPriority.checkedRadioButtonId) {
            R.id.rbHighPriority -> "HIGH"
            R.id.rbMediumPriority -> "MEDIUM"
            R.id.rbLowPriority -> "LOW"
            else -> "MEDIUM" // Default to Medium if none selected
        }

        val dueDateTime = if (dueDate != null && dueTime != null) {
            LocalDateTime.of(dueDate!!, dueTime!!)
        } else {
            currentTodo!!.dueDate // Use original if not set
        }

        val reminderDateTime = if (switchReminder.isChecked && reminderDate != null && reminderTime != null) {
            LocalDateTime.of(reminderDate!!, reminderTime!!)
        } else if (!switchReminder.isChecked) {
            null
        } else {
            currentTodo!!.reminderDateTime // Use original if incomplete
        }

        editTodoViewModel.checkForUnsavedChanges(
            title = title,
            description = description,
            priority = priority,
            dueDateTime = dueDateTime,
            reminderDateTime = reminderDateTime
        )
    }

    private fun showDatePicker(isForReminder: Boolean) {
       val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (isForReminder) {
                    reminderDate = selectedDate
                    tvReminderDate.text = selectedDate.format(dateFormatter)
                } else {
                    dueDate = selectedDate
                    tvSelectedDate.text = selectedDate.format(dateFormatter)
                }
                checkForUnsavedChanges()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker(isForReminder: Boolean) {
       val now = LocalTime.now()

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = LocalTime.of(hourOfDay, minute)
                if (isForReminder) {
                    reminderTime = selectedTime
                    tvReminderTime.text = selectedTime.format(timeFormatter)
                } else {
                    dueTime = selectedTime
                    tvSelectedTime.text = selectedTime.format(timeFormatter)
                }
                checkForUnsavedChanges()
            },
            now.hour,
            now.minute,
            false
        )
        timePickerDialog.show()
    }

    private fun resetReminderViews() {
        tvReminderDate.text = "Select Date"
        tvReminderTime.text = "Select Time"
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        checkExactAlarmPermission()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "✅ Notification permission granted", Toast.LENGTH_SHORT).show()
            checkExactAlarmPermission()
        } else {
            Toast.makeText(this, "⚠️ Notification permission denied. Reminders may not work.", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkExactAlarmPermission() {
        if (!editTodoViewModel.checkReminderPermissions()) {
            AlertDialog.Builder(this)
                .setTitle("Precise Reminders")
                .setMessage("For exact reminder timing, please allow precise alarms in the next screen.")
                .setPositiveButton("Open Settings") { _, _ ->
                    editTodoViewModel.requestReminderPermissions()
                }
                .setNegativeButton("Skip") { _, _ ->
                    Toast.makeText(this, "⚠️ Reminders may be delayed without precise timing", Toast.LENGTH_LONG).show()
                }
                .show()
        }
    }

//    override fun onBackPressed() {
//        super.onBackPressed()
//        handleBackPress()
//    }

}