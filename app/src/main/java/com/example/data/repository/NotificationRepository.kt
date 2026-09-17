package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(context: Context) {
    private val notificationDao = AppDatabase.getInstance(context).notificationDao()

    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val unreadCount: Flow<Int> = notificationDao.getUnreadCount()

    suspend fun insertNotification(notification: NotificationEntity): Long {
        return notificationDao.insertNotification(notification)
    }

    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun markAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    suspend fun deleteNotification(id: Long) {
        notificationDao.deleteNotification(id)
    }

    suspend fun clearAll() {
        notificationDao.clearAll()
    }
}
