package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationEntity
import com.example.data.model.AdminNotice
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminNoticeRepository(private val context: Context) {

    private val _activeNotice = MutableStateFlow<AdminNotice?>(null)
    val activeNotice: StateFlow<AdminNotice?> = _activeNotice.asStateFlow()

    private val dismissedNoticeIds = mutableSetOf<String>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val notificationDao by lazy { AppDatabase.getInstance(context).notificationDao() }

    init {
        listenToRemoteNotices()
    }

    private fun listenToRemoteNotices() {
        try {
            val database = FirebaseDatabase.getInstance()
            val noticeRef = database.getReference("admin_notice")

            noticeRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        _activeNotice.value = null
                        return
                    }

                    try {
                        val id = snapshot.child("id").getValue(String::class.java)
                            ?: snapshot.key ?: "remote_notice_${System.currentTimeMillis()}"
                        val title = snapshot.child("title").getValue(String::class.java) ?: ""
                        val message = snapshot.child("message").getValue(String::class.java)
                            ?: snapshot.child("body").getValue(String::class.java) ?: ""
                        val isActive = snapshot.child("isActive").getValue(Boolean::class.java)
                            ?: snapshot.child("active").getValue(Boolean::class.java)
                            ?: (title.isNotBlank() || message.isNotBlank())
                        val timestamp = snapshot.child("timestamp").getValue(Long::class.java)
                            ?: System.currentTimeMillis()
                        val type = snapshot.child("type").getValue(String::class.java) ?: "info"
                        val actionText = snapshot.child("actionText").getValue(String::class.java)
                        val actionUrl = snapshot.child("actionUrl").getValue(String::class.java)
                        val isDismissible = snapshot.child("isDismissible").getValue(Boolean::class.java) ?: true

                        if (isActive && (title.isNotBlank() || message.isNotBlank()) && !dismissedNoticeIds.contains(id)) {
                            val notice = AdminNotice(
                                id = id,
                                title = title,
                                message = message,
                                timestamp = timestamp,
                                isActive = true,
                                type = type,
                                actionText = actionText,
                                actionUrl = actionUrl,
                                isDismissible = isDismissible
                            )
                            _activeNotice.value = notice

                            // Save to local Notification Room database if new
                            scope.launch {
                                try {
                                    val entity = NotificationEntity(
                                        remoteMessageId = id,
                                        title = if (title.isNotBlank()) title else "प्रशासनिक सूचना (Admin Notice)",
                                        body = message,
                                        timestamp = timestamp,
                                        isRead = false,
                                        type = "admin_notice",
                                        linkUrl = actionUrl
                                    )
                                    notificationDao.insertNotification(entity)
                                } catch (e: Exception) {
                                    Log.w("AdminNoticeRepo", "Could not persist admin notice to DB: ${e.message}")
                                }
                            }
                        } else {
                            _activeNotice.value = null
                        }
                    } catch (e: Exception) {
                        Log.e("AdminNoticeRepo", "Error parsing remote admin notice: ${e.message}")
                        _activeNotice.value = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminNoticeRepo", "Database listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.w("AdminNoticeRepo", "Firebase Realtime Database init: ${e.message}")
            _activeNotice.value = null
        }
    }

    fun dismissNotice(noticeId: String) {
        dismissedNoticeIds.add(noticeId)
        if (_activeNotice.value?.id == noticeId) {
            _activeNotice.value = null
        }
    }
}
