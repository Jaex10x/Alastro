package com.example.decena

data class Task(
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val date: Long, // timestamp
    val time: String,
    val priority: String, // High, Medium, Low
    val category: String,
    val isCompleted: Boolean = false
)