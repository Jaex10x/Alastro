package com.example.decena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TasksViewModelFactory(private val databaseHelper: TaskDatabaseHelper) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TasksViewModel().apply {
                initializeDatabase(databaseHelper)
            } as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}