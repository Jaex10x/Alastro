package com.example.decena

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    // Views
    private lateinit var imgProfile: ImageView
    private lateinit var monthTextView: TextView
    private lateinit var arrowDown: ImageView
    private lateinit var tasksContainer: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter

    private lateinit var viewModel: TasksViewModel
    private lateinit var databaseHelper: TaskDatabaseHelper

    private var selectedDate: Calendar = Calendar.getInstance()
    private val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerView()
        setupViewModel()
        setupClickListeners()

        updateMonthDisplay()
    }

    private fun initializeViews(view: View) {
        // Initialize views
        imgProfile = view.findViewById(R.id.imgProfile)
        monthTextView = view.findViewById(R.id.monthTextView)
        arrowDown = view.findViewById(R.id.arrowDown)
        tasksContainer = view.findViewById(R.id.tasksContainer)
    }

    private fun setupRecyclerView() {
        recyclerView = RecyclerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutManager = LinearLayoutManager(requireContext())
        }

        tasksContainer.removeAllViews()
        tasksContainer.addView(recyclerView)

        taskAdapter = TaskAdapter(
            tasks = emptyList(),
            onTaskCheckedListener = { task, isChecked ->
                viewModel.updateTaskCompletion(task.id, isChecked)
            },
            onTaskMoreClickListener = { task, view ->
                showTaskOptionsMenu(task, view)
            }
        )
        recyclerView.adapter = taskAdapter
    }

    private fun setupViewModel() {
        databaseHelper = TaskDatabaseHelper(requireContext())
        val factory = TasksViewModelFactory(databaseHelper)
        viewModel = ViewModelProvider(requireActivity(), factory)[TasksViewModel::class.java]

        // Observe tasks
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.updateTasks(tasks)
        }

        // Observe selected date
        viewModel.selectedDate.observe(viewLifecycleOwner) { dateInMillis ->
            selectedDate.timeInMillis = dateInMillis
            updateMonthDisplay()
        }
    }

    private fun setupClickListeners() {
        monthTextView.setOnClickListener { showDatePicker() }
        arrowDown.setOnClickListener { showDatePicker() }

        imgProfile.setOnClickListener {
            (activity as? MainActivity)?.navigateToProfile()
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = android.app.DatePickerDialog(
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

    private fun updateMonthDisplay() {
        monthTextView.text = monthFormatter.format(selectedDate.time)
    }

    private fun showTaskOptionsMenu(task: Task, anchor: View) {
        val popupMenu = android.widget.PopupMenu(requireContext(), anchor)
        popupMenu.menu.add("Delete")

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                "Delete" -> {
                    viewModel.deleteTask(task)
                    Snackbar.make(requireView(), "Task deleted", Snackbar.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }
}