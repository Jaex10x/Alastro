package com.example.decena

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class TasksFragment : Fragment() {

    private lateinit var tasksContainer: LinearLayout
    private lateinit var btnAddTask: ImageView
    private lateinit var imgProfile: ImageView
    private lateinit var todayTextView: TextView
    private lateinit var recyclerView: RecyclerView
    // Change this to TimelineTaskAdapter
    private lateinit var taskAdapter: TimelineTaskAdapter
    private lateinit var viewModel: TasksViewModel
    private lateinit var databaseHelper: TaskDatabaseHelper

    private var selectedDate: Calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            initializeViews(view)
            setupRecyclerView()
            setupViewModel()
            setupClickListeners(view)

            // Add this line to debug database contents
            debugDatabaseContents()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews(view: View) {
        tasksContainer = view.findViewById(R.id.tasksContainer)
        btnAddTask = view.findViewById(R.id.btnAddTask)
        imgProfile = view.findViewById(R.id.imgProfile)

        // Get the first child (Today text)
        if (tasksContainer.childCount > 0) {
            todayTextView = tasksContainer.getChildAt(0) as TextView
        } else {
            todayTextView = TextView(requireContext()).apply {
                text = "Today"
                textSize = 18f
                setTextColor(resources.getColor(android.R.color.black, null))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            tasksContainer.addView(todayTextView)
        }
    }

    private fun setupRecyclerView() {
        recyclerView = RecyclerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Clear all views and add back the Today text and RecyclerView
        tasksContainer.removeAllViews()
        tasksContainer.addView(todayTextView)
        tasksContainer.addView(recyclerView)

        // Initialize TimelineTaskAdapter (not TaskAdapter)
        taskAdapter = TimelineTaskAdapter(
            tasks = emptyList(),
            onTaskCheckedListener = { task, isChecked ->
                viewModel.updateTaskCompletion(task.id, isChecked)
            }
            // Note: TimelineTaskAdapter doesn't have edit/delete listeners
        )
        recyclerView.adapter = taskAdapter
    }

    private fun setupViewModel() {
        databaseHelper = TaskDatabaseHelper(requireContext())
        val factory = TasksViewModelFactory(databaseHelper)
        viewModel = ViewModelProvider(requireActivity(), factory).get(TasksViewModel::class.java)

        // Observe tasks - this will trigger whenever tasks change
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            Log.d("TasksFragment", "Received ${tasks.size} tasks")
            updateTasksList(tasks)
        }

        // Observe selected date
        viewModel.selectedDate.observe(viewLifecycleOwner) { dateInMillis ->
            selectedDate.timeInMillis = dateInMillis
            updateDateDisplay()
            Log.d("TasksFragment", "Date changed to: $dateInMillis")
        }
    }

    private fun setupClickListeners(view: View) {
        btnAddTask.setOnClickListener {
            showTaskDialog(null)
        }

        imgProfile.setOnClickListener {
            try {
                (activity as? MainActivity)?.navigateToProfile()
            } catch (e: Exception) {
                Toast.makeText(context, "Profile clicked", Toast.LENGTH_SHORT).show()
            }
        }

        todayTextView.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                viewModel.setSelectedDate(selectedDate.timeInMillis)
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateDisplay() {
        val dateText = when {
            isToday(selectedDate) -> "Today"
            isTomorrow(selectedDate) -> "Tomorrow"
            isYesterday(selectedDate) -> "Yesterday"
            else -> dateFormatter.format(selectedDate.time)
        }
        todayTextView.text = dateText
    }

    private fun updateTasksList(tasks: List<Task>) {
        // Filter tasks for the selected date
        val tasksForSelectedDate = tasks.filter { task ->
            val taskCalendar = Calendar.getInstance().apply {
                timeInMillis = task.date
            }
            taskCalendar.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    taskCalendar.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
        }

        // Update the "Today" text with task count
        val baseText = if (todayTextView.text.contains("(")) {
            todayTextView.text.toString().substringBefore(" (")
        } else {
            todayTextView.text.toString()
        }
        todayTextView.text = "$baseText (${tasksForSelectedDate.size} tasks)"

        // Update adapter with filtered tasks
        taskAdapter.updateTasks(tasksForSelectedDate)

        Log.d("TasksFragment", "UI updated with ${tasksForSelectedDate.size} tasks for selected date")
    }

    private fun showTaskDialog(existingTask: Task?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val etTaskName = dialogView.findViewById<EditText>(R.id.etTaskName)

        // Simple dialog with just task name for testing
        AlertDialog.Builder(requireContext())
            .setTitle(if (existingTask == null) "New Task" else "Edit Task")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val taskText = etTaskName.text.toString().trim()

                if (taskText.isEmpty()) {
                    Toast.makeText(requireContext(), "Task name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Use current date and time
                val now = System.currentTimeMillis()
                val calendar = Calendar.getInstance().apply { timeInMillis = now }
                val timeStr = String.format("%d:%02d %s",
                    calendar.get(Calendar.HOUR_OF_DAY).let { if (it > 12) it - 12 else if (it == 0) 12 else it },
                    calendar.get(Calendar.MINUTE),
                    if (calendar.get(Calendar.HOUR_OF_DAY) >= 12) "PM" else "AM"
                )

                val newTask = Task(
                    id = existingTask?.id ?: 0,
                    title = taskText,
                    description = existingTask?.description ?: "",
                    date = now,
                    time = timeStr,
                    priority = existingTask?.priority ?: "Medium",
                    category = existingTask?.category ?: "General",
                    isCompleted = existingTask?.isCompleted ?: false
                )

                if (existingTask == null) {
                    viewModel.addTask(newTask)
                    Toast.makeText(requireContext(), "Adding: $taskText", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.deleteTask(existingTask)
                    viewModel.addTask(newTask)
                    Toast.makeText(requireContext(), "Updating: $taskText", Toast.LENGTH_SHORT).show()
                }

                // Show count of tasks after operation
                viewModel.loadTasks()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun isTomorrow(calendar: Calendar): Boolean {
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(calendar: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
    }
    private fun debugDatabaseContents() {
        // Force a fresh load from database
        viewModel.loadTasks()

        // Also query directly to see all tasks
        val allTasks = databaseHelper.getAllTasks()
        Log.d("TasksFragment", "=== DIRECT DATABASE QUERY ===")
        Log.d("TasksFragment", "Total tasks in DB: ${allTasks.size}")
        allTasks.forEach { task ->
            Log.d("TasksFragment", "DB Task: id=${task.id}, title='${task.title}', date=${task.date}, time='${task.time}'")
        }
    }
}