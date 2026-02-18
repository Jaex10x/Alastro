package com.example.decena

import android.net.Uri

data class Profile(
    val id: Int = 1,
    val username: String = "User Name",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val about: String = "",
    val avatarUri: String = "" // Store URI as string
)