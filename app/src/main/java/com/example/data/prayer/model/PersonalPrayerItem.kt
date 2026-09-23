package com.example.data.prayer.model

data class PersonalPrayerItem(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val details: String = "",
    val createdDate: String,
    val isAnswered: Boolean = false,
    val answeredDate: String? = null,
    val testimonyNotes: String? = null
)
