package com.example.data.bible.model

data class BibleResourceModule(
    val id: String = "",
    val titleHindi: String = "",
    val titleEnglish: String = "",
    val resourceType: String = "TRANSLATION", // TRANSLATION, COMMENTARY, DICTIONARY, STUDY_GUIDE
    val language: String = "Hindi",
    val fileFormat: String = "ZIP", // ZIP, JSON, SQLITE, PDF
    val fileName: String = "",
    val downloadUrl: String = "",
    val storagePath: String = "",
    val fileSizeBytes: Long = 0L,
    val description: String = "",
    val version: String = "1.0",
    val authorOrSource: String = "",
    val isActive: Boolean = true,
    val uploadedByAdmin: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val downloadCount: Int = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "titleHindi" to titleHindi,
        "titleEnglish" to titleEnglish,
        "resourceType" to resourceType,
        "language" to language,
        "fileFormat" to fileFormat,
        "fileName" to fileName,
        "downloadUrl" to downloadUrl,
        "storagePath" to storagePath,
        "fileSizeBytes" to fileSizeBytes,
        "description" to description,
        "version" to version,
        "authorOrSource" to authorOrSource,
        "isActive" to isActive,
        "uploadedByAdmin" to uploadedByAdmin,
        "timestamp" to timestamp,
        "downloadCount" to downloadCount
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): BibleResourceModule {
            return BibleResourceModule(
                id = id,
                titleHindi = map["titleHindi"] as? String ?: "",
                titleEnglish = map["titleEnglish"] as? String ?: "",
                resourceType = map["resourceType"] as? String ?: "TRANSLATION",
                language = map["language"] as? String ?: "Hindi",
                fileFormat = map["fileFormat"] as? String ?: "ZIP",
                fileName = map["fileName"] as? String ?: "",
                downloadUrl = map["downloadUrl"] as? String ?: "",
                storagePath = map["storagePath"] as? String ?: "",
                fileSizeBytes = (map["fileSizeBytes"] as? Number)?.toLong() ?: 0L,
                description = map["description"] as? String ?: "",
                version = map["version"] as? String ?: "1.0",
                authorOrSource = map["authorOrSource"] as? String ?: "",
                isActive = map["isActive"] as? Boolean ?: true,
                uploadedByAdmin = map["uploadedByAdmin"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                downloadCount = (map["downloadCount"] as? Number)?.toInt() ?: 0
            )
        }
    }
}
