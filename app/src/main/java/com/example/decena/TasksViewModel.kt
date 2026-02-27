package com.example.decena

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TasksViewModel(private val databaseHelper: TaskDatabaseHelper) : ViewModel() {

    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    private val _selectedDate = MutableLiveData<Long>()
    val selectedDate: LiveData<Long> = _selectedDate

    init {
        // Set default selected date to today
        _selectedDate.value = System.currentTimeMillis()
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            val allTasks = withContext(Dispatchers.IO) {
                databaseHelper.getAllTasks()
            }
            _tasks.postValue(allTasks)
        }
    }

    fun loadTasksForDate(dateInMillis: Long) {
        viewModelScope.launch {
            val tasksForDate = withContext(Dispatchers.IO) {
                databaseHelper.getTasksForDate(dateInMillis)
            }
            _tasks.postValue(tasksForDate)
        }
    }

    fun loadTasksForDateRange(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            val tasksInRange = withContext(Dispatchers.IO) {
                databaseHelper.getTasksForDateRange(startDate, endDate)
            }
            _tasks.postValue(tasksInRange)
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                databaseHelper.addTask(task)
            }
            loadTasks()
        }
    }

    fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                databaseHelper.updateTaskCompletion(taskId, isCompleted)
            }
            loadTasks()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                databaseHelper.deleteTask(task.id)
            }
            loadTasks()
        }
    }

    fun setSelectedDate(dateInMillis: Long) {
        _selectedDate.value = dateInMillis
    }
}