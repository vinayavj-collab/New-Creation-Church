package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.AppReleaseVersion
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AppReleaseRepository private constructor() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val releasesCollection by lazy { firestore.collection("app_releases") }
    private val latestDocRef by lazy { firestore.collection("app_releases").document("latest_release") }

    fun observeAllReleases(): Flow<List<AppReleaseVersion>> = callbackFlow {
        val listener = releasesCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.d(TAG, "Releases snapshot offline/unavailable: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    if (doc.id == "latest_release") null
                    else doc.data?.let { AppReleaseVersion.fromMap(doc.id, it) }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun observeLatestActiveRelease(): Flow<AppReleaseVersion?> = callbackFlow {
        val listener = latestDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.d(TAG, "Latest release snapshot offline/unavailable: ${error.message}")
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val data = snapshot.data
                if (data != null) {
                    trySend(AppReleaseVersion.fromMap(snapshot.id, data))
                } else {
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getLatestActiveRelease(): AppReleaseVersion? = withContext(Dispatchers.IO) {
        try {
            val snapshot = latestDocRef.get().await()
            if (snapshot.exists()) {
                val data = snapshot.data
                if (data != null) {
                    return@withContext AppReleaseVersion.fromMap(snapshot.id, data)
                }
            }
            // Fallback query if latest_release summary not written yet
            val querySnapshot = releasesCollection
                .whereEqualTo("isActive", true)
                .orderBy("versionCode", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            val doc = querySnapshot.documents.firstOrNull { it.id != "latest_release" }
            doc?.data?.let { AppReleaseVersion.fromMap(doc.id, it) }
        } catch (e: Exception) {
            Log.d(TAG, "Latest active release not available or client is offline: ${e.message}")
            null
        }
    }

    fun uploadApkAndPublishRelease(
        context: Context,
        fileUri: Uri,
        version: AppReleaseVersion,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(fileUri)
                    ?: throw IllegalArgumentException("फ़ाइल खोली नहीं जा सकी")

                val safeFileName = if (version.apkFileName.isNotBlank()) {
                    version.apkFileName
                } else {
                    "app-v${version.versionName}-c${version.versionCode}.apk"
                }
                val storageRef = storage.reference.child("app_releases/$safeFileName")

                val metadata = StorageMetadata.Builder()
                    .setContentType("application/vnd.android.package-archive")
                    .setCustomMetadata("versionCode", version.versionCode.toString())
                    .setCustomMetadata("versionName", version.versionName)
                    .build()

                val uploadTask = storageRef.putStream(inputStream, metadata)

                uploadTask.addOnProgressListener { taskSnapshot ->
                    val total = taskSnapshot.totalByteCount
                    val transferred = taskSnapshot.bytesTransferred
                    if (total > 0) {
                        val progress = transferred.toFloat() / total.toFloat()
                        onProgress(progress)
                    }
                }

                uploadTask.await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                val finalVersion = version.copy(
                    downloadUrl = downloadUrl,
                    storagePath = storageRef.path,
                    apkFileName = safeFileName
                )

                // If marked active, mark other releases inactive
                if (finalVersion.isActive) {
                    val currentReleases = releasesCollection.get().await()
                    val batch = firestore.batch()
                    for (d in currentReleases.documents) {
                        if (d.id != "latest_release") {
                            batch.update(d.reference, "isActive", false)
                        }
                    }
                    val newDocRef = if (finalVersion.id.isNotBlank()) {
                        releasesCollection.document(finalVersion.id)
                    } else {
                        releasesCollection.document()
                    }
                    val versionWithId = finalVersion.copy(id = newDocRef.id)
                    batch.set(newDocRef, versionWithId.toMap())
                    batch.set(latestDocRef, versionWithId.toMap())
                    batch.commit().await()
                } else {
                    val newDocRef = if (finalVersion.id.isNotBlank()) {
                        releasesCollection.document(finalVersion.id)
                    } else {
                        releasesCollection.document()
                    }
                    val versionWithId = finalVersion.copy(id = newDocRef.id)
                    newDocRef.set(versionWithId.toMap()).await()
                }

                withContext(Dispatchers.Main) {
                    onComplete(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading APK and publishing release", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, e.localizedMessage ?: "अपलोड विफल रहा")
                }
            }
        }
    }

    fun setActiveRelease(release: AppReleaseVersion, onComplete: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentReleases = releasesCollection.get().await()
                val batch = firestore.batch()
                for (d in currentReleases.documents) {
                    if (d.id != "latest_release") {
                        val isThis = d.id == release.id
                        batch.update(d.reference, "isActive", isThis)
                    }
                }
                val updatedRelease = release.copy(isActive = true)
                batch.set(latestDocRef, updatedRelease.toMap())
                batch.commit().await()

                withContext(Dispatchers.Main) {
                    onComplete(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting active release", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, e.localizedMessage ?: "सक्रिय करने में त्रुटि")
                }
            }
        }
    }

    fun toggleForceUpdate(release: AppReleaseVersion, isForce: Boolean, onComplete: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val batch = firestore.batch()
                val docRef = releasesCollection.document(release.id)
                batch.update(docRef, "isForceUpdate", isForce)
                if (release.isActive) {
                    batch.update(latestDocRef, "isForceUpdate", isForce)
                }
                batch.commit().await()

                withContext(Dispatchers.Main) {
                    onComplete(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling force update", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, e.localizedMessage ?: "Force Update अपडेट करने में त्रुटि")
                }
            }
        }
    }

    fun deleteRelease(release: AppReleaseVersion, onComplete: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Delete from Firebase Storage if path exists
                if (release.storagePath.isNotBlank()) {
                    try {
                        storage.reference.child(release.storagePath).delete().await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Storage delete warning: ${e.message}")
                    }
                }

                // Delete doc from Firestore
                releasesCollection.document(release.id).delete().await()

                // If this was active, find another latest release to make active
                if (release.isActive) {
                    val remaining = releasesCollection
                        .orderBy("versionCode", Query.Direction.DESCENDING)
                        .limit(2)
                        .get()
                        .await()
                    val nextActiveDoc = remaining.documents.firstOrNull { it.id != "latest_release" && it.id != release.id }
                    if (nextActiveDoc != null) {
                        val nextData = nextActiveDoc.data
                        if (nextData != null) {
                            val nextVersion = AppReleaseVersion.fromMap(nextActiveDoc.id, nextData).copy(isActive = true)
                            val batch = firestore.batch()
                            batch.update(nextActiveDoc.reference, "isActive", true)
                            batch.set(latestDocRef, nextVersion.toMap())
                            batch.commit().await()
                        }
                    } else {
                        latestDocRef.delete().await()
                    }
                }

                withContext(Dispatchers.Main) {
                    onComplete(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting release", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, e.localizedMessage ?: "डिलीट करने में त्रुटि")
                }
            }
        }
    }

    companion object {
        private const val TAG = "AppReleaseRepository"

        @Volatile
        private var instance: AppReleaseRepository? = null

        fun getInstance(): AppReleaseRepository {
            return instance ?: synchronized(this) {
                instance ?: AppReleaseRepository().also { instance = it }
            }
        }
    }
}
