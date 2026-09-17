package com.example.data.bible.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ExternalModuleImporter {

    suspend fun importExternalFolder(context: Context, appDb: BibleDatabase, treeUriString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (treeUriString.isBlank()) return@withContext Result.success(0)
            val treeUri = Uri.parse(treeUriString)
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext Result.failure(Exception("Invalid tree URI"))

            // Automatically create and target sub-folders like "Bibles" and "Commentaries"
            var biblesDir = rootDoc.findFile("Bibles")
            if (biblesDir == null || !biblesDir.isDirectory) {
                biblesDir = rootDoc.createDirectory("Bibles")
            }
            var commentariesDir = rootDoc.findFile("Commentaries")
            if (commentariesDir == null || !commentariesDir.isDirectory) {
                commentariesDir = rootDoc.createDirectory("Commentaries")
            }

            val allFiles = mutableListOf<DocumentFile>()
            collectFilesRecursive(rootDoc, allFiles)
            biblesDir?.let { collectFilesRecursive(it, allFiles) }
            commentariesDir?.let { collectFilesRecursive(it, allFiles) }

            var totalImported = 0

            for (docFile in allFiles) {
                if (!docFile.isFile) continue
                val fileName = docFile.name ?: continue
                val lowerName = fileName.lowercase()

                val isSupported = lowerName.endsWith(".db") ||
                        lowerName.endsWith(".sqlite") ||
                        lowerName.endsWith(".sqlite3") ||
                        lowerName.endsWith(".bblx") ||
                        lowerName.endsWith(".bbli") ||
                        lowerName.endsWith(".mybible") ||
                        lowerName.endsWith(".topx") ||
                        lowerName.endsWith(".dctx") ||
                        lowerName.endsWith(".dct.mybible") ||
                        lowerName.endsWith(".bok.mybible") ||
                        lowerName.endsWith(".jor.mynmbible") ||
                        lowerName.endsWith(".jor.mybible") ||
                        lowerName.endsWith(".zip")

                if (!isSupported) continue

                try {
                    val tempFile = File(context.cacheDir, "ext_mod_${System.currentTimeMillis()}_$fileName")
                    context.contentResolver.openInputStream(docFile.uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: continue

                    if (lowerName.endsWith(".zip")) {
                        try {
                            tempFile.inputStream().use { zipStream ->
                                val zipResult = Sqlite3BibleImporter.importZipBibleFile(context, appDb, zipStream)
                                if (zipResult.isSuccess) {
                                    totalImported += zipResult.getOrDefault(0)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        val translationId = fileName.substringBeforeLast(".").substringBeforeLast(".")
                        val result = importModuleFileSafely(context, appDb, tempFile, translationId, lowerName)
                        if (result.isSuccess) {
                            totalImported += result.getOrDefault(0)
                        }
                    }
                    tempFile.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Result.success(totalImported)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun collectFilesRecursive(dir: DocumentFile, accumulator: MutableList<DocumentFile>) {
        try {
            for (file in dir.listFiles()) {
                if (file.isDirectory) {
                    collectFilesRecursive(file, accumulator)
                } else if (file.isFile) {
                    accumulator.add(file)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun importModuleFileSafely(
        context: Context,
        appDb: BibleDatabase,
        dbFile: File,
        baseTranslationId: String,
        lowerFileName: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        Sqlite3BibleImporter.importSqliteBibleFile(
            context = context,
            appDb = appDb,
            sourceUri = android.net.Uri.fromFile(dbFile),
            targetTranslationId = baseTranslationId,
            replaceExisting = false
        )
    }
}
