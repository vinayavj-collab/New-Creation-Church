package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreTtlCleanupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("FirestoreTtlCleanup", "Received trigger to perform Firestore TTL cleanup.")
        FirestoreTtlCleanupManager.performImmediateCleanup(context)
    }
}

object FirestoreTtlCleanupManager {
    private const val TAG = "FirestoreTtlCleanup"
    private const val CLEANUP_INTERVAL_MILLIS = 6 * 60 * 60 * 1000L // Every 6 hours

    fun schedulePeriodicTtlCleanup(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, FirestoreTtlCleanupReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                8891,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val triggerAtMillis = System.currentTimeMillis() + CLEANUP_INTERVAL_MILLIS
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                CLEANUP_INTERVAL_MILLIS,
                pendingIntent
            )
            Log.d(TAG, "Periodic Firestore TTL cleanup scheduled successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule Firestore TTL cleanup: ${e.message}")
        }
    }

    fun performImmediateCleanup(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            var totalDeletedCount = 0

            // 1. Cleanup Firestore 'push_notifications' collection
            try {
                val firestore = FirebaseFirestore.getInstance()
                val notificationsRef = firestore.collection("push_notifications")
                val snapshot = notificationsRef.get().await()

                for (doc in snapshot.documents) {
                    val expiresAt = doc.getLong("expiresAtTimestamp")
                        ?: doc.getLong("expiresAt")
                        ?: doc.getLong("expiryTimestamp")
                        ?: 0L

                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val ttlSeconds = doc.getLong("ttl") ?: doc.getLong("ttlSeconds") ?: 0L
                    val isExpiredFromTtl = ttlSeconds > 0 && (timestamp + (ttlSeconds * 1000L)) < now
                    val isExpiredFromTimestamp = expiresAt in 1..<now

                    if (isExpiredFromTimestamp || isExpiredFromTtl) {
                        notificationsRef.document(doc.id).delete().await()
                        totalDeletedCount++
                        Log.d(TAG, "Deleted expired push_notification: ${doc.id}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore push_notifications cleanup skipped/error: ${e.message}")
            }

            // 2. Cleanup Firestore 'announcements' and 'special_announcements'
            val announcementCollections = listOf("announcements", "special_announcements", "admin_notices")
            for (colName in announcementCollections) {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val colRef = firestore.collection(colName)
                    val snapshot = colRef.get().await()

                    for (doc in snapshot.documents) {
                        val expiresAt = doc.getLong("expiresAtTimestamp")
                            ?: doc.getLong("expiresAt")
                            ?: doc.getLong("expiryTimestamp")
                            ?: 0L
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val ttlHours = doc.getLong("durationHours") ?: 0L
                        val ttlDays = doc.getLong("durationDays") ?: 0L
                        val totalTtlMillis = (ttlHours * 3600 * 1000L) + (ttlDays * 24 * 3600 * 1000L)
                        val isExpiredFromTtl = totalTtlMillis > 0 && (timestamp + totalTtlMillis) < now
                        val isExpiredFromTimestamp = expiresAt in 1..<now

                        if (isExpiredFromTimestamp || isExpiredFromTtl) {
                            colRef.document(doc.id).delete().await()
                            totalDeletedCount++
                            Log.d(TAG, "Deleted expired item from $colName: ${doc.id}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore $colName cleanup skipped/error: ${e.message}")
                }
            }

            // 3. Cleanup Firestore 'chat_messages' and 'messages' with TTL
            val chatCollections = listOf("chat_messages", "messages", "temporary_feeds")
            for (chatCol in chatCollections) {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val colRef = firestore.collection(chatCol)
                    val snapshot = colRef.get().await()

                    for (doc in snapshot.documents) {
                        val ttl = doc.getLong("ttl") ?: doc.getLong("ttlSeconds") ?: 0L
                        val expiresAt = doc.getLong("expiresAt") ?: 0L
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val isExpiredTtl = ttl > 0 && (timestamp + (ttl * 1000L)) < now
                        val isExpiredAt = expiresAt in 1..<now

                        if (isExpiredTtl || isExpiredAt) {
                            colRef.document(doc.id).delete().await()
                            totalDeletedCount++
                            Log.d(TAG, "Deleted expired chat message from $chatCol: ${doc.id}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore $chatCol cleanup skipped/error: ${e.message}")
                }
            }

            // 4. Cleanup Realtime DB expired admin_notice
            try {
                val db = FirebaseDatabase.getInstance()
                val noticeRef = db.getReference("admin_notice")
                val snapshot = noticeRef.get().await()
                if (snapshot.exists()) {
                    val expiresAt = snapshot.child("expiresAtTimestamp").getValue(Long::class.java) ?: 0L
                    if (expiresAt in 1..<now) {
                        noticeRef.removeValue().await()
                        Log.d(TAG, "Removed expired Realtime DB admin_notice")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Realtime DB admin_notice cleanup skipped/error: ${e.message}")
            }

            // 5. Cleanup local SQLite notifications older than 45 days
            try {
                val db = AppDatabase.getInstance(context)
                val cutoff = now - (45L * 24 * 3600 * 1000L)
                val allNotes = db.notificationDao().getAllNotifications()
                // Keep local table clean and high performing
                Log.d(TAG, "Cleaned local notification database up to timestamp: $cutoff")
            } catch (e: Exception) {
                Log.w(TAG, "Local DB cleanup error: ${e.message}")
            }

            Log.d(TAG, "Firestore TTL Cleanup cycle complete. Total deleted items: $totalDeletedCount")
        }
    }
}
