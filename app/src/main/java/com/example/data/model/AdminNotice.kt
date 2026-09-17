package com.example.data.model

data class AdminNotice(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isActive: Boolean = false,
    val type: String = "info", // "info", "warning", "announcement", "urgent"
    val actionText: String? = null,
    val actionUrl: String? = null,
    val isDismissible: Boolean = true
)
