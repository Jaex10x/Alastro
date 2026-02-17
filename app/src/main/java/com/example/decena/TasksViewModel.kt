package com.example.decena

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class TasksViewModel : ViewModel() {

    private val _tasks = MutableLiveData<List<Task>>(emptyList())
    val tasks: LiveData<List<Task>> = _tasks

    private val _selectedDate = MutableLiveData<Long>(System.currentTimeMillis())
    val selectedDate: LiveData<Long> = _selectedDate

    private lateinit var databaseHelper: TaskDatabaseHelper

    fun initializeDatabase(helper: TaskDatabaseHelper) {
        this.databaseHelper = helper
        loadTasksForSelectedDate()
    }

    fun setSelectedDate(dateInMillis: Long) {
        _selectedDate.value = dateInMillis
        loadTasksForSelectedDate()
    }

    fun loadTasksForSelectedDate() {
        val date = _selectedDate.value ?: return

        viewModelScope.launch {
            val tasks = withContext(Dispatchers.IO) {
                databaseHelper.getTasksForDate(date)
            }
            _tasks.postValue(tasks)
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                databaseHelper.addTask(task)
            }
            loadTasksForSelectedDate()
        }
    }

    fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                databaseHelper.updateTaskCompletion(taskId, isCompleted)
            }
            loadTasksForSelectedDate()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                databaseHelper.deleteTask(task.id)
            }
            loadTasksForSelectedDate()
        }
    }
}