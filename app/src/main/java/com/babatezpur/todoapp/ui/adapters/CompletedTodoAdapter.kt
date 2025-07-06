package com.babatezpur.todoapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.babatezpur.todoapp.R
import com.babatezpur.todoapp.data.entities.Priority
import com.babatezpur.todoapp.data.entities.Todo

class CompletedTodoAdapter(
    private val completedTodoList : MutableList<Todo>,
    private val onTodoClick: (Todo, Int) -> Unit,
    private val onTodoUncheck : (Todo, Int) -> Unit
) : RecyclerView.Adapter<CompletedTodoAdapter.CompletedTodoViewHolder>() {
    private val dateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")

    inner class CompletedTodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardView)
        val todoTitle: TextView = itemView.findViewById(R.id.todo_title)
        val todoDescription: TextView = itemView.findViewById(R.id.todo_description)
        val todoDueDate: TextView = itemView.findViewById(R.id.todo_due_date)
        val checkBoxComplete: CheckBox = itemView.findViewById(R.id.checkbox_completed)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletedTodoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_active_todo, parent, false)
        return CompletedTodoViewHolder(view)
    }

    override fun getItemCount(): Int  = completedTodoList.size

    override fun onBindViewHolder(holder: CompletedTodoViewHolder, position: Int) {
        val todo = completedTodoList[position]
        holder.todoTitle.text = todo.title
        holder.todoDescription.text = todo.description
        holder.todoDueDate.text = "Completed : ${todo.dueDate.format(dateTimeFormatter)}"

        holder.checkBoxComplete.isChecked = todo.isCompleted

        styleAsCompleted(holder)

        setCompletedPriorityBackground(holder.cardView, todo.priority)

        holder.itemView.setOnClickListener {
            onTodoClick(todo, position)
        }

        holder.checkBoxComplete.setOnCheckedChangeListener(null) // Clear previous listener
        holder.checkBoxComplete.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) { // Only trigger if unchecked
                onTodoUncheck(todo, position)
            }
        }
    }

    private fun styleAsCompleted(holder: CompletedTodoViewHolder) {
        val context = holder.itemView.context

        holder.todoTitle.paintFlags = holder.todoTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

        // Mute text colors
        holder.todoTitle.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        holder.todoDescription.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        holder.todoDueDate.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))

        // Reduce overall alpha for "faded" effect
        holder.cardView.alpha = 0.8f
    }

    private fun setCompletedPriorityBackground(cardView: CardView, priority: Priority) {
        val context = cardView.context
        val colorRes = when (priority) {
            Priority.P1 -> R.color.priority_high_bg_muted      // Muted red
            Priority.P2 -> R.color.priority_medium_bg_muted    // Muted orange
            Priority.P3 -> R.color.priority_low_bg_muted       // Muted green
        }

        // Use regular colors if muted colors don't exist in your project
        val fallbackColorRes = when (priority) {
            Priority.P1 -> R.color.priority_high_bg
            Priority.P2 -> R.color.priority_medium_bg
            Priority.P3 -> R.color.priority_low_bg
        }

        try {
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, colorRes))
        } catch (e: Exception) {
            // Fallback to regular colors
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, fallbackColorRes))
        }
    }

    /**
     * 🔄 Update the entire list of completed todos
     */
    fun updateCompletedTodos(newTodos: List<Todo>) {
        completedTodoList.clear()
        completedTodoList.addAll(newTodos)
        notifyDataSetChanged()
    }

    /**
     * 🗑️ Remove todo from list (when unchecked)
     */
    fun removeTodo(position: Int) {
        if (position >= 0 && position < completedTodoList.size) {
            completedTodoList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

}