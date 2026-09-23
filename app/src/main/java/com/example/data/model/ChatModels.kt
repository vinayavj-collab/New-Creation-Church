package com.example.data.model

data class ChatMessage(
    val id: String = "",
    val roomId: String = "general_congregation",
    val senderId: String = "",
    val senderName: String = "",
    val senderDesignation: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val expireAt: Long = System.currentTimeMillis() + (72 * 3600 * 1000L),
    val isAnnouncement: Boolean = false,
    val isPinned: Boolean = false
)
