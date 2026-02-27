package com.example.decena

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var btnMonthSelector: TextView
    private lateinit var imgProfile: ImageView
    private lateinit var tasksContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var recyclerView: RecyclerView
    private lateinit var timelineAdapter: TimelineTaskAdapter
    private lateinit var viewModel: TasksViewModel
    private lateinit var databaseHelper: TaskDatabaseHelper
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var tvNoTasks: TextView

    private var selectedDate: Calendar = Calendar.getInstance()
    private val monthFormatter = SimpleDateFormat("MMMM", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            initializeViews(view)
            setupRecyclerView()
            setupViewModels()
            setupClickListeners(view)

            // Load tasks for current month
            loadTasksForCurrentMonth()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews(view: View) {
        btnMonthSelector = view.findViewById(R.id.btnMonthSelector)
        imgProfile = view.findViewById(R.id.imgProfile)
        tvNoTasks = view.findViewById(R.id.tvNoTasks)

        // Find ScrollView
        scrollView = view.findViewById(R.id.scrollView)

        // Get the LinearLayout inside ScrollView
        tasksContainer = scrollView.getChildAt(0) as LinearLayout

        // Clear any existing views
        tasksContainer.removeAllViews()
    }

    private fun setupRecyclerView() {
        recyclerView = RecyclerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutManager = LinearLayoutManager(requireContext())
        }

        tasksContainer.addView(recyclerView)

        timelineAdapter = TimelineTaskAdapter(
            tasks = emptyList(),
            onTaskCheckedListener = { task, isChecked ->
                viewModel.updateTaskCompletion(task.id, isChecked)
                // Show feedback
                val message = if (isChecked) "Task completed!" else "Task marked pending"
                Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
            }
        )
        recyclerView.adapter = timelineAdapter
    }

    private fun setupViewModels() {
        databaseHelper = TaskDatabaseHelper(requireContext())
        val factory = TasksViewModelFactory(databaseHelper)
        viewModel = ViewModelProvider(requireActivity(), factory).get(TasksViewModel::class.java)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Observe all tasks
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            // Filter tasks for current month
            val monthTasks = filterTasksForCurrentMonth(tasks)
            timelineAdapter.updateTasks(monthTasks)

            // Show/hide no tasks message
            updateNoTasksVisibility(monthTasks)
        }

        viewModel.selectedDate.observe(viewLifecycleOwner) { dateInMillis ->
            selectedDate.timeInMillis = dateInMillis
            updateMonthDisplay()
            loadTasksForCurrentMonth()
        }

        sharedViewModel.currentMonth.observe(viewLifecycleOwner) { newMonth ->
            btnMonthSelector.text = newMonth
        }
    }

    private fun setupClickListeners(view: View) {
        btnMonthSelector.setOnClickListener {
            try {
                (activity as? MainActivity)?.navigateToCalendar()
            } catch (e: Exception) {
                Toast.makeText(context, "Calendar clicked", Toast.LENGTH_SHORT).show()
            }
        }

        imgProfile.setOnClickListener {
            try {
                (activity as? MainActivity)?.navigateToProfile()
            } catch (e: Exception) {
                Toast.makeText(context, "Profile clicked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTasksForCurrentMonth() {
        // Get the start of the month (first day at 00:00:00)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calendar.timeInMillis

        // Get the end of the month (last day at 23:59:59)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.timeInMillis

        // Load tasks for the month range
        viewModel.loadTasksForDateRange(startOfMonth, endOfMonth)
    }
    private fun filterTasksForCurrentMonth(tasks: List<Task>): List<Task> {
        val calendar = Calendar.getInstance()

        return tasks.filter { task ->
            calendar.timeInMillis = task.date
            calendar.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                    calendar.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR)
        }.sortedBy { task -> task.date } // Sort by date
    }

    private fun updateNoTasksVisibility(tasks: List<Task>) {
        if (tasks.isEmpty()) {
            tvNoTasks.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvNoTasks.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateMonthDisplay() {
        val month = monthFormatter.format(selectedDate.time)
        sharedViewModel.setMonth(month)
    }
}