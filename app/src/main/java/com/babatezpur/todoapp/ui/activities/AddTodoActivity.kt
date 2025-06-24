package com.babatezpur.todoapp.ui.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.babatezpur.todoapp.R
import com.babatezpur.todoapp.data.database.TodoDatabase
import com.babatezpur.todoapp.data.repositories.TodoRepository
import com.babatezpur.todoapp.domain.managers.TodoManager
import com.babatezpur.todoapp.viewmodels.AddTodoViewModel
import com.babatezpur.todoapp.viewmodels.AddTodoViewModelFactory
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class AddTodoActivity : AppCompatActivity() {

//    private lateinit var toolbar: Toolbar
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
    private lateinit var btnSaveTodo: Button

    private lateinit var addTodoViewModel: AddTodoViewModel

    private var dueDate: LocalDate? = null
    private var dueTime: LocalTime? = null

    private var reminderDate: LocalDate? = null
    private var reminderTime: LocalTime? = null

    // Formatters
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    override fun onCreate(savedInstanceState: Bundle?) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_todo)

        setupViewModel()

        initViews()
        setupClickListeners()

        observeViewModel()
    }

    private fun setupViewModel() {
        val database = TodoDatabase.getDatabase(this)
        val repository = TodoRepository(database.todoDao())
        val todoManager = TodoManager(repository)

        val factory = AddTodoViewModelFactory(todoManager)
        addTodoViewModel = ViewModelProvider(this, factory)[AddTodoViewModel::class.java]

    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun initViews() {
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
        btnSaveTodo = findViewById(R.id.btnSaveTodo)
    }

    private fun setupClickListeners() {
        tvSelectedDate.setOnClickListener {
            showDatePicker(isForReminder = false)
        }

        tvSelectedTime.setOnClickListener {
            showTimePicker(isForReminder = false)
        }

        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                layoutReminderDetails.visibility = LinearLayout.VISIBLE
                Toast.makeText(this, "Please set reminder date and time", Toast.LENGTH_SHORT).show()
            } else {
                layoutReminderDetails.visibility = LinearLayout.GONE
                reminderDate = null
                reminderTime = null
                resetReminderViews()
            }
        }
        tvReminderDate.setOnClickListener {
            showDatePicker(isForReminder = true)
        }

        tvReminderTime.setOnClickListener {
            showTimePicker(isForReminder = true)
        }

        btnSaveTodo.setOnClickListener {
            saveTodo()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            addTodoViewModel.uiState.collect { state ->
                when {
                    state.isLoading -> {
                        btnSaveTodo.isEnabled = false
                        btnSaveTodo.text = "Saving..."

                    }
                    state.isSuccess -> {
                        Toast.makeText(this@AddTodoActivity, "Todo saved successfully", Toast.LENGTH_SHORT).show()
                        finish() // Close the activity after saving
                    }
                    state.error != null -> {
                        btnSaveTodo.isEnabled = true
                        btnSaveTodo.text = "Save Todo"
                        Toast.makeText(this@AddTodoActivity, "Error: ${state.error}", Toast.LENGTH_LONG).show()
                        addTodoViewModel.clearError()
                    }

                    else -> {
                        btnSaveTodo.isEnabled = true
                        btnSaveTodo.text = "Save Todo"
                    }
                }
            }
        }
    }

    private fun resetReminderViews() {
        tvReminderDate.text = "Select Date"
        tvReminderTime.text = "Select Time"
    }

    private fun saveTodo() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val priority = when (rgPriority.checkedRadioButtonId) {
            R.id.rbHighPriority -> "High"
            R.id.rbMediumPriority -> "Medium"
            R.id.rbLowPriority -> "Low"
            else -> "Medium" // Default priority
        }

        if (title.isEmpty()) {
            tilTitle.error = "Title cannot be empty"
            return
        } else {
            tilTitle.error = null
        }

        if (dueDate == null) {
            Toast.makeText(this, "Please select due date and time", Toast.LENGTH_SHORT).show()
            return
        }

        if (switchReminder.isChecked && (reminderDate == null || reminderTime == null)) {
            Toast.makeText(this, "Please set reminder date and time", Toast.LENGTH_SHORT).show()
            return
        }

        // Combine date and time when saving
        val dueDateTime = LocalDateTime.of(dueDate!!, dueTime!!)
        val reminderDateTime = if (switchReminder.isChecked && reminderDate != null && reminderTime != null) {
            LocalDateTime.of(reminderDate!!, reminderTime!!)
        } else null

        addTodoViewModel.createTodo(
            title = title,
            description = description,
            priority = priority,
            dueDateTime = dueDateTime,
            reminderDateTime = reminderDateTime
        )
        finish() // Close the activity after saving
    }

    private fun showDatePicker(isForReminder : Boolean) {
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
                    tvSelectedDate.text = selectedDate?.format(dateFormatter)
                }
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
            },
            now.hour,
            now.minute,
            false // Use 24-hour format
        )
        timePickerDialog.show()
    }



}