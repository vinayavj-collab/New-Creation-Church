package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE is_read = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE app_notifications SET is_read = 1 WHERE is_read = 0")
    suspend fun markAllAsRead()

    @Query("UPDATE app_notifications SET is_read = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    @Query("DELETE FROM app_notifications")
    suspend fun clearAll()
}
