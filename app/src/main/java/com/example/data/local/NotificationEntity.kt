package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "remote_message_id")
    val remoteMessageId: String? = null,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,
    val type: String = "general",
    @ColumnInfo(name = "link_url")
    val linkUrl: String? = null
)
