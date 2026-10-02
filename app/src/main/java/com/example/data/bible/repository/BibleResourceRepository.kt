package com.example.data.bible.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.data.bible.model.BibleResourceModule
import com.google.firebase.firestore.FieldValue
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
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class BibleResourceRepository private constructor() {

    private val firestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            FirebaseFirestore.getInstance()
        }
    }
    private val storage by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (_: Exception) {
            FirebaseStorage.getInstance()
        }
    }
    private val resourcesCollection by lazy { firestore.collection("bible_resources") }

    fun observeAllResources(): Flow<List<BibleResourceModule>> = callbackFlow {
        val listener = resourcesCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.d(TAG, "Observe resources error/offline: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { BibleResourceModule.fromMap(doc.id, it) }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun observeActiveResources(): Flow<List<BibleResourceModule>> = callbackFlow {
        val listener = resourcesCollection
            .whereEqualTo("isActive", true)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.d(TAG, "Observe active resources offline: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { BibleResourceModule.fromMap(doc.id, it) }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun uploadResourceFile(
        context: Context,
        fileUri: Uri,
        resource: BibleResourceModule,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(fileUri)
                    ?: throw IllegalArgumentException("फ़ाइल खोली नहीं जा सकी")

                val safeFileName = if (resource.fileName.isNotBlank()) {
                    resource.fileName
                } else {
                    "${resource.resourceType.lowercase()}_${System.currentTimeMillis()}.${resource.fileFormat.lowercase()}"
                }

                val storageRef = storage.reference.child("bible_resources/$safeFileName")

                val contentType = when (resource.fileFormat.uppercase()) {
                    "ZIP" -> "application/zip"
                    "JSON" -> "application/json"
                    "PDF" -> "application/pdf"
                    "SQLITE", "DB" -> "application/x-sqlite3"
                    else -> "application/octet-stream"
                }

                val metadata = StorageMetadata.Builder()
                    .setContentType(contentType)
                    .setCustomMetadata("resourceType", resource.resourceType)
                    .setCustomMetadata("language", resource.language)
                    .build()

                val uploadTask = storageRef.putStream(inputStream, metadata)

                uploadTask.addOnProgressListener { taskSnapshot ->
                    val total = taskSnapshot.totalByteCount
                    val transferred = taskSnapshot.bytesTransferred
                    if (total > 0) {
                        onProgress(transferred.toFloat() / total.toFloat())
                    }
                }

                uploadTask.await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                val docRef = if (resource.id.isNotBlank()) {
                    resourcesCollection.document(resource.id)
                } else {
                    resourcesCollection.document()
                }

                val finalResource = resource.copy(
                    id = docRef.id,
                    downloadUrl = downloadUrl,
                    storagePath = storageRef.path,
                    fileName = safeFileName
                )

                docRef.set(finalResource.toMap()).await()

                withContext(Dispatchers.Main) {
                    onComplete(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading Bible resource", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, e.localizedMessage ?: "अपलोड विफल रहा")
                }
            }
        }
    }

    fun toggleResourceActive(resource: BibleResourceModule, isActive: Boolean, onComplete: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                resourcesCollection.document(resource.id).update("isActive", isActive).await()
                withContext(Dispatchers.Main) { onComplete(true, null) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(false, e.localizedMessage) }
            }
        }
    }

    fun deleteResource(resource: BibleResourceModule, onComplete: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (resource.storagePath.isNotBlank()) {
                    try {
                        storage.reference.child(resource.storagePath).delete().await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Delete storage warning: ${e.message}")
                    }
                }
                resourcesCollection.document(resource.id).delete().await()
                withContext(Dispatchers.Main) { onComplete(true, null) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(false, e.localizedMessage) }
            }
        }
    }

    fun downloadAndSaveResource(
        context: Context,
        resource: BibleResourceModule,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean, File?, String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Increment download count in Firestore
                try {
                    resourcesCollection.document(resource.id)
                        .update("downloadCount", FieldValue.increment(1))
                } catch (_: Exception) {}

                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir
                val bibleDir = File(downloadsDir, "bible_modules").apply { mkdirs() }
                val targetFile = File(bibleDir, resource.fileName)

                val url = URL(resource.downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                val fileLength = connection.contentLength
                val input = connection.inputStream
                val output = FileOutputStream(targetFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toInt()
                        withContext(Dispatchers.Main) { onProgress(progress) }
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                withContext(Dispatchers.Main) {
                    onComplete(true, targetFile, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading Bible resource", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, null, e.localizedMessage ?: "डाउनलोड विफल रहा")
                }
            }
        }
    }

    companion object {
        private const val TAG = "BibleResourceRepo"

        @Volatile
        private var instance: BibleResourceRepository? = null

        fun getInstance(): BibleResourceRepository {
            return instance ?: synchronized(this) {
                instance ?: BibleResourceRepository().also { instance = it }
            }
        }
    }
}
