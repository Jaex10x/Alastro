package com.example.decena

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TimelineTaskAdapter(
    private var tasks: List<Task>,
    private val onTaskCheckedListener: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TimelineTaskAdapter.TimelineViewHolder>() {

    private val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    private val weekDayFormat = SimpleDateFormat("E", Locale.getDefault())

    class TimelineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTaskDay: TextView = itemView.findViewById(R.id.tvTaskDay)
        val tvTaskWeekDay: TextView = itemView.findViewById(R.id.tvTaskWeekDay)
        val tvTaskTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvTaskTime: TextView = itemView.findViewById(R.id.tvTaskTime)
        val cbTask: CheckBox = itemView.findViewById(R.id.cbTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timeline_task, parent, false)
        return TimelineViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        val task = tasks[position]

        // Format date for display (day and weekday)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = task.date
        }
        holder.tvTaskDay.text = dayFormat.format(calendar.time)
        holder.tvTaskWeekDay.text = weekDayFormat.format(calendar.time)

        // Task details
        holder.tvTaskTitle.text = task.title
        holder.tvTaskTime.text = task.time

        // Checkbox state
        holder.cbTask.isChecked = task.isCompleted

        // Remove previous listener to avoid memory leaks
        holder.cbTask.setOnCheckedChangeListener(null)

        // Set new listener
        holder.cbTask.setOnCheckedChangeListener { _, isChecked ->
            onTaskCheckedListener(task, isChecked)
        }

        // Visual feedback for completed tasks
        if (task.isCompleted) {
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTaskTitle.alpha = 0.5f
            holder.tvTaskTime.alpha = 0.5f
            holder.tvTaskDay.alpha = 0.5f
            holder.tvTaskWeekDay.alpha = 0.5f
            holder.cbTask.alpha = 0.5f
        } else {
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTaskTitle.alpha = 1.0f
            holder.tvTaskTime.alpha = 1.0f
            holder.tvTaskDay.alpha = 1.0f
            holder.tvTaskWeekDay.alpha = 1.0f
            holder.cbTask.alpha = 1.0f
        }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}