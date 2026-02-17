package com.example.decena

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private var tasks: List<Task>,
    private val onTaskCheckedListener: (Task, Boolean) -> Unit,
    private val onTaskMoreClickListener: (Task, View) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Match with your item_task_row.xml IDs
        val cbTask: CheckBox = itemView.findViewById(R.id.cbTask)
        val tvTaskTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvTaskDate: TextView = itemView.findViewById(R.id.tvTaskDate)
        val imgMore: ImageView = itemView.findViewById(R.id.imgMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_row, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        // Set task title
        holder.tvTaskTitle.text = task.title

        // Format and set date/time
        val taskDateTime = formatTaskDateTime(task.date, task.time)
        holder.tvTaskDate.text = taskDateTime

        // Set checkbox state
        holder.cbTask.isChecked = task.isCompleted

        // Apply strikethrough and opacity if task is completed
        if (task.isCompleted) {
            holder.tvTaskTitle.paint.isStrikeThruText = true
            holder.tvTaskTitle.alpha = 0.6f
            holder.tvTaskDate.alpha = 0.6f
        } else {
            holder.tvTaskTitle.paint.isStrikeThruText = false
            holder.tvTaskTitle.alpha = 1.0f
            holder.tvTaskDate.alpha = 1.0f
        }

        // Handle checkbox click
        holder.cbTask.setOnCheckedChangeListener { _, isChecked ->
            onTaskCheckedListener(task, isChecked)
        }

        // Handle more button click
        holder.imgMore.setOnClickListener { view ->
            onTaskMoreClickListener(task, view)
        }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    private fun formatTaskDateTime(dateInMillis: Long, time: String): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateInMillis
        }

        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }

        return when {
            isSameDay(calendar, today) -> "Today, $time"
            isSameDay(calendar, tomorrow) -> "Tomorrow, $time"
            isSameDay(calendar, yesterday) -> "Yesterday, $time"
            else -> {
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                "${dateFormat.format(calendar.time)}, $time"
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}