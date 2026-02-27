package com.example.decena

data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val date: Long,
    val time: String,
    val priority: String,
    val category: String,
    val isCompleted: Boolean = false
)