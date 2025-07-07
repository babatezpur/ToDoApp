package com.babatezpur.todoapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.babatezpur.todoapp.R
import com.babatezpur.todoapp.data.entities.Priority
import com.babatezpur.todoapp.data.entities.Todo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Problem : todo disappears as soon as it is completed, we need to show the checked sign for a few seconds before removing it from the list
class TodoAdapter(
    private val todoList : MutableList<Todo>,
    private val onTodoClick : (Todo, Int) -> Unit,
    private val onTodoComplete : (Todo, Int, Boolean) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")


    // Define a ViewHolder class to hold the views for each item
    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardView)
        val todoTitle: TextView = itemView.findViewById(R.id.todo_title)
        val todoDescription: TextView = itemView.findViewById(R.id.todo_description)
        val todoDueDate: TextView = itemView.findViewById(R.id.todo_due_date)
        val checkBoxComplete: CheckBox = itemView.findViewById(R.id.checkbox_completed)
        val iconOverdue: ImageView = itemView.findViewById(R.id.icon_overdue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view  = LayoutInflater.from(parent.context).inflate(R.layout.item_active_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = todoList[position]

        holder.todoTitle.text = todo.title
        holder.todoDescription.text = todo.description
        holder.todoDueDate.text = todo.dueDate.format(dateTimeFormatter).toString() // Format as needed
        holder.checkBoxComplete.isChecked = todo.isCompleted

        handleOverdueStatus(holder, todo)

        // Set card background color based on priority
        setPriorityBackground(holder.cardView, todo.priority)
        holder.itemView.setOnClickListener {
            onTodoClick(todo, position)
        }
        // Handle checkbox completion with proper listener management
        holder.checkBoxComplete.setOnCheckedChangeListener(null) // Clear previous listener
        holder.checkBoxComplete.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != todo.isCompleted) { // Only trigger if state actually changed
                onTodoComplete(todo, position, isChecked)
            }
        }
    }

    override fun getItemCount(): Int {
        // Return the size of your data set
        return todoList.size
    }

    private fun setPriorityBackground(cardView: CardView, priority: Priority) {
        val context = cardView.context
        val colorRes = when (priority) {
            Priority.P1 -> R.color.priority_high_bg      // Light red (High priority)
            Priority.P2 -> R.color.priority_medium_bg    // Light orange/yellow (Medium)
            Priority.P3 -> R.color.priority_low_bg       // Light green/blue (Low)
        }
        cardView.setCardBackgroundColor(ContextCompat.getColor(context, colorRes))
    }

    // Method to update specific todo (useful for delayed updates)
    fun updateTodo(position: Int, updatedTodo: Todo) {
        if (position >= 0 && position < todoList.size) {
            todoList[position] = updatedTodo
            notifyItemChanged(position)
        }
    }

    // Method to remove todo with animation
    fun removeTodo(position: Int) {
        if (position >= 0 && position < todoList.size) {
            todoList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // Method to update the entire list
    fun updateTodos(newTodos: List<Todo>) {
        todoList.clear()
        todoList.addAll(newTodos)
        notifyDataSetChanged()
    }

    private fun handleOverdueStatus(holder: TodoViewHolder, todo: Todo) {
        val isOverdue = todo.dueDate.isBefore(LocalDateTime.now()) && !todo.isCompleted

        if (isOverdue) {
            holder.iconOverdue.visibility = View.VISIBLE

            holder.todoDueDate.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
            )
        } else {
            holder.iconOverdue.visibility = View.GONE

            holder.todoDueDate.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.black)
            )
        }
    }
}