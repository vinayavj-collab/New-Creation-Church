package com.example.data.model

data class AppReleaseVersion(
    val id: String = "",
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkFileName: String = "",
    val downloadUrl: String = "",
    val storagePath: String = "",
    val fileSizeBytes: Long = 0L,
    val releaseNotes: String = "",
    val isForceUpdate: Boolean = false,
    val isActive: Boolean = true,
    val uploadedByAdmin: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val abiType: String = "Universal"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "versionCode" to versionCode,
        "versionName" to versionName,
        "apkFileName" to apkFileName,
        "downloadUrl" to downloadUrl,
        "storagePath" to storagePath,
        "fileSizeBytes" to fileSizeBytes,
        "releaseNotes" to releaseNotes,
        "isForceUpdate" to isForceUpdate,
        "isActive" to isActive,
        "uploadedByAdmin" to uploadedByAdmin,
        "timestamp" to timestamp,
        "abiType" to abiType
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AppReleaseVersion {
            return AppReleaseVersion(
                id = id,
                versionCode = (map["versionCode"] as? Number)?.toInt() ?: 0,
                versionName = map["versionName"] as? String ?: "",
                apkFileName = map["apkFileName"] as? String ?: "",
                downloadUrl = map["downloadUrl"] as? String ?: "",
                storagePath = map["storagePath"] as? String ?: "",
                fileSizeBytes = (map["fileSizeBytes"] as? Number)?.toLong() ?: 0L,
                releaseNotes = map["releaseNotes"] as? String ?: "",
                isForceUpdate = map["isForceUpdate"] as? Boolean ?: false,
                isActive = map["isActive"] as? Boolean ?: false,
                uploadedByAdmin = map["uploadedByAdmin"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                abiType = map["abiType"] as? String ?: "Universal"
            )
        }
    }
}
