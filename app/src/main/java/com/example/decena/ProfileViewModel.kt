package com.example.decena

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel : ViewModel() {

    private val _profile = MutableLiveData<Profile>()
    val profile: LiveData<Profile> = _profile

    private lateinit var databaseHelper: ProfileDatabaseHelper

    fun initializeDatabase(helper: ProfileDatabaseHelper) {
        this.databaseHelper = helper
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val profile = withContext(Dispatchers.IO) {
                databaseHelper.getProfile()
            }
            _profile.postValue(profile)
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                databaseHelper.updateProfile(profile)
            }
            if (success) {
                loadProfile() // Reload to get updated data
            }
        }
    }
}