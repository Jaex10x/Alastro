package com.example.decena
import com.google.android.material.button.MaterialButton
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class TasksFragment : Fragment() {

    private lateinit var tasksContainer: LinearLayout
    private lateinit var todayTextView: TextView
    private lateinit var btnAddTask: MaterialButton
    private lateinit var imgProfile: ImageView
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

        initializeViews(view)
        setupViewModel()
        setupClickListeners(view)
    }

    private fun initializeViews(view: View) {
        tasksContainer = view.findViewById(R.id.tasksContainer)
        btnAddTask = view.findViewById(R.id.btnAddTask)
        imgProfile = view.findViewById(R.id.imgProfile)

        // Get the first child of tasksContainer which is the "Today" TextView
        todayTextView = tasksContainer.getChildAt(0) as TextView
    }

    private fun setupViewModel() {
        databaseHelper = TaskDatabaseHelper(requireContext())
        val factory = TasksViewModelFactory(databaseHelper)
        viewModel = ViewModelProvider(requireActivity(), factory)[TasksViewModel::class.java]

        // Observe tasks from ViewModel
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            updateTasksList(tasks)
        }

        // Observe selected date
        viewModel.selectedDate.observe(viewLifecycleOwner) { dateInMillis ->
            selectedDate.timeInMillis = dateInMillis
            updateDateDisplay()
        }
    }

    private fun setupClickListeners(view: View) {
        btnAddTask.setOnClickListener {
            showTaskDialog(null)
        }

        imgProfile.setOnClickListener {
            (activity as? MainActivity)?.navigateToProfile()
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
        // Remove all views except the first one (Today TextView)
        while (tasksContainer.childCount > 1) {
            tasksContainer.removeViewAt(1)
        }

        // Update the "Today" text with task count
        val dateText = todayTextView.text.toString()
        todayTextView.text = "$dateText (${tasks.size} tasks)"

        // Add each task to the container
        for (task in tasks) {
            addTaskView(task)
        }
    }

    private fun addTaskView(task: Task) {
        val inflater = LayoutInflater.from(context)
        val taskView = inflater.inflate(R.layout.item_task_row, tasksContainer, false)

        val tvTitle = taskView.findViewById<TextView>(R.id.tvTaskTitle)
        val tvDate = taskView.findViewById<TextView>(R.id.tvTaskDate)
        val checkBox = taskView.findViewById<CheckBox>(R.id.cbTask)
        val imgMore = taskView.findViewById<ImageView>(R.id.imgMore)

        tvTitle.text = task.title
        tvDate.text = formatTaskDateTime(task.date, task.time)
        checkBox.isChecked = task.isCompleted

        if (task.isCompleted) {
            tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            tvTitle.alpha = 0.5f
            tvDate.alpha = 0.5f
        } else {
            tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            tvTitle.alpha = 1.0f
            tvDate.alpha = 1.0f
        }

        checkBox.setOnClickListener {
            viewModel.updateTaskCompletion(task.id, checkBox.isChecked)
        }

        imgMore.setOnClickListener {
            showTaskOptionsMenu(task, taskView, it)
        }

        tasksContainer.addView(taskView)
    }

    private fun showTaskDialog(existingTask: Task?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)

        val etTaskName = dialogView.findViewById<EditText>(R.id.etTaskName)
        val btnDate = dialogView.findViewById<Button>(R.id.btnPickDate)
        val btnTime = dialogView.findViewById<Button>(R.id.btnPickTime)

        var selectedDateInMillis = selectedDate.timeInMillis
        var selectedDateStr = ""
        var selectedTimeStr = ""

        if (existingTask != null) {
            etTaskName.setText(existingTask.title)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = existingTask.date
            }
            selectedDateInMillis = existingTask.date
            selectedDateStr = dateFormatter.format(calendar.time)
            btnDate.text = selectedDateStr
            selectedTimeStr = existingTask.time
            btnTime.text = selectedTimeStr
        }

        btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDateInMillis = calendar.timeInMillis
                selectedDateStr = dateFormatter.format(calendar.time)
                btnDate.text = selectedDateStr
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val amPm = if (hour >= 12) "PM" else "AM"
                val hour12 = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
                val minStr = String.format("%02d", minute)
                selectedTimeStr = "$hour12:$minStr $amPm"
                btnTime.text = selectedTimeStr
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (existingTask == null) "New Task" else "Edit Task")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val taskText = etTaskName.text.toString().trim()

                if (taskText.isEmpty()) {
                    Toast.makeText(requireContext(), "Task name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (selectedDateStr.isEmpty()) {
                    selectedDateStr = dateFormatter.format(Date(selectedDateInMillis))
                }

                if (selectedTimeStr.isEmpty()) {
                    selectedTimeStr = "12:00 PM"
                }

                if (existingTask == null) {
                    val newTask = Task(
                        title = taskText,
                        description = "",
                        date = selectedDateInMillis,
                        time = selectedTimeStr,
                        priority = "Medium",
                        category = "General",
                        isCompleted = false
                    )
                    viewModel.addTask(newTask)
                    Snackbar.make(requireView(), "Task added", Snackbar.LENGTH_SHORT).show()
                } else {
                    // For edit, delete old and add new
                    viewModel.deleteTask(existingTask)
                    val updatedTask = existingTask.copy(
                        title = taskText,
                        date = selectedDateInMillis,
                        time = selectedTimeStr
                    )
                    viewModel.addTask(updatedTask)
                    Snackbar.make(requireView(), "Task updated", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTaskOptionsMenu(task: Task, taskView: View, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add("Edit")
        popup.menu.add("Delete")
        popup.menu.add(if (task.isCompleted) "Mark Pending" else "Mark Complete")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Delete" -> {
                    viewModel.deleteTask(task)
                    Snackbar.make(requireView(), "Task deleted", Snackbar.LENGTH_SHORT).show()
                    true
                }
                "Edit" -> {
                    showTaskDialog(task)
                    true
                }
                "Mark Complete", "Mark Pending" -> {
                    viewModel.updateTaskCompletion(task.id, !task.isCompleted)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun formatTaskDateTime(dateInMillis: Long, time: String): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateInMillis
        }

        return when {
            isToday(calendar) -> "Today, $time"
            isTomorrow(calendar) -> "Tomorrow, $time"
            isYesterday(calendar) -> "Yesterday, $time"
            else -> "${dateFormatter.format(calendar.time)}, $time"
        }
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
}