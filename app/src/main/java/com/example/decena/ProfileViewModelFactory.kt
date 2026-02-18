package com.example.decena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ProfileViewModelFactory(private val databaseHelper: ProfileDatabaseHelper) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel().apply {
                initializeDatabase(databaseHelper)
            } as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}