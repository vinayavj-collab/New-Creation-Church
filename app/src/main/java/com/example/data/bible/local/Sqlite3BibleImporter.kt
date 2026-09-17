package com.example.data.bible.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object Sqlite3BibleImporter {

    private val MYBIBLE_BOOK_MAP = mapOf(
        10 to 1, 20 to 2, 30 to 3, 40 to 4, 50 to 5, 60 to 6, 70 to 7, 80 to 8, 90 to 9, 100 to 10,
        110 to 11, 120 to 12, 130 to 13, 140 to 14, 150 to 15, 160 to 16, 190 to 17, 220 to 18,
        230 to 19, 240 to 20, 250 to 21, 260 to 22, 290 to 23, 300 to 24, 310 to 25, 330 to 26,
        340 to 27, 350 to 28, 360 to 29, 370 to 30, 380 to 31, 390 to 32, 400 to 33, 410 to 34,
        420 to 35, 430 to 36, 440 to 37, 450 to 38, 460 to 39, 470 to 40, 480 to 41, 490 to 42,
        500 to 43, 510 to 44, 520 to 45, 530 to 46, 540 to 47, 550 to 48, 560 to 49, 570 to 50,
        580 to 51, 590 to 52, 600 to 53, 610 to 54, 620 to 55, 630 to 56, 640 to 57, 650 to 58,
        660 to 59, 670 to 60, 680 to 61, 690 to 62, 700 to 63, 710 to 64, 720 to 65, 730 to 66
    )

    fun normalizeBookId(rawBookNumber: Int): Int {
        if (rawBookNumber in 1..66) return rawBookNumber
        return MYBIBLE_BOOK_MAP[rawBookNumber] ?: when {
            rawBookNumber in 10..730 && MYBIBLE_BOOK_MAP.containsKey(rawBookNumber) -> MYBIBLE_BOOK_MAP[rawBookNumber]!!
            else -> rawBookNumber
        }
    }
    suspend fun clearAllTranslations(db: BibleDatabase) = withContext(Dispatchers.IO) {
        db.bibleDao().clearAllVerses()
    }

    /**
     * Clears a specific translation from local database.
     */
    suspend fun clearTranslation(db: BibleDatabase, translationId: String) = withContext(Dispatchers.IO) {
        db.bibleDao().deleteVersesByTranslation(translationId)
    }

    /**
     * Imports a Zip file containing one or more SQLite3 (.db / .sqlite) files.
     */
    suspend fun importZipBibleFile(
        context: Context,
        appDb: BibleDatabase,
        inputStream: InputStream
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var totalVerses = 0
            val zipInput = ZipInputStream(inputStream)
            var entry = zipInput.nextEntry
            val tempDir = File(context.cacheDir, "unzipped_bibles")
            if (!tempDir.exists()) tempDir.mkdirs()

            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && (name.endsWith(".db", true) || name.endsWith(".sqlite", true) || name.endsWith(".sqlite3", true) || name.endsWith(".mybible", true))) {
                    val rawFileName = name.substringAfterLast("/")
                    val targetTransId = when {
                        rawFileName.contains(".bbl.mybible", ignoreCase = true) -> rawFileName.substring(0, rawFileName.indexOf(".bbl.mybible", ignoreCase = true))
                        rawFileName.contains(".mybible", ignoreCase = true) -> rawFileName.substring(0, rawFileName.indexOf(".mybible", ignoreCase = true))
                        else -> rawFileName.substringBeforeLast(".")
                    }
                    val outFile = File(tempDir, "$targetTransId.db")
                    FileOutputStream(outFile).use { out ->
                        zipInput.copyTo(out)
                    }
                    val result = importSqliteFileDirect(context, appDb, outFile, targetTransId, true)
                    if (result.isSuccess) {
                        totalVerses += result.getOrDefault(0)
                    }
                    outFile.delete()
                }
                zipInput.closeEntry()
                entry = zipInput.nextEntry
            }
            zipInput.close()
            Result.success(totalVerses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun importSqliteFileDirect(
        context: Context,
        appDb: BibleDatabase,
        dbFile: File,
        targetTranslationId: String,
        replaceExisting: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        var sqliteDb: SQLiteDatabase? = null
        try {
            sqliteDb = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

            val cursorTables = sqliteDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            val allTables = mutableListOf<String>()
            if (cursorTables.moveToFirst()) {
                do {
                    val name = cursorTables.getString(0)
                    if (!name.startsWith("sqlite_") && name != "android_metadata") {
                        allTables.add(name)
                    }
                } while (cursorTables.moveToNext())
            }
            cursorTables.close()

            val isCommentaryFile = targetTranslationId.contains("commentar", ignoreCase = true) ||
                    allTables.any { it.contains("commentar", ignoreCase = true) }

            // 1. Process commentary table if present
            val commentaryTableName = allTables.firstOrNull { it.contains("commentar", ignoreCase = true) }
            if (commentaryTableName != null) {
                val linkedTranslationId = targetTranslationId.removeSuffix("_commentaries").removeSuffix(".commentaries")
                if (replaceExisting) {
                    appDb.bibleDao().deleteCommentariesByTranslation(linkedTranslationId)
                }

                val commColCursor = sqliteDb.rawQuery("PRAGMA table_info($commentaryTableName)", null)
                val commCols = mutableListOf<String>()
                if (commColCursor.moveToFirst()) {
                    do {
                        commCols.add(commColCursor.getString(1))
                    } while (commColCursor.moveToNext())
                }
                commColCursor.close()

                val bCol = commCols.firstOrNull { it.equals("book", true) || it.equals("book_number", true) || it.equals("book_id", true) || it.equals("b", true) } ?: "book_number"
                val cFromCol = commCols.firstOrNull { it.equals("chapter_number_from", true) || it.equals("chapter_from", true) || it.equals("chapter", true) || it.equals("c", true) } ?: "chapter_number_from"
                val vFromCol = commCols.firstOrNull { it.equals("verse_number_from", true) || it.equals("verse_from", true) || it.equals("verse", true) || it.equals("v", true) } ?: "verse_number_from"
                val cToCol = commCols.firstOrNull { it.equals("chapter_number_to", true) || it.equals("chapter_to", true) }
                val vToCol = commCols.firstOrNull { it.equals("verse_number_to", true) || it.equals("verse_to", true) }
                val markerCol = commCols.firstOrNull { it.equals("marker", true) }
                val textColComm = commCols.firstOrNull { it.equals("text", true) || it.equals("commentary", true) || it.equals("comment", true) } ?: "text"

                val availableSelectCols = mutableListOf<String>()
                if (commCols.contains(bCol)) availableSelectCols.add(bCol)
                if (commCols.contains(cFromCol)) availableSelectCols.add(cFromCol)
                if (commCols.contains(vFromCol)) availableSelectCols.add(vFromCol)
                if (cToCol != null && commCols.contains(cToCol)) availableSelectCols.add(cToCol)
                if (vToCol != null && commCols.contains(vToCol)) availableSelectCols.add(vToCol)
                if (markerCol != null && commCols.contains(markerCol)) availableSelectCols.add(markerCol)
                if (commCols.contains(textColComm)) availableSelectCols.add(textColComm)

                if (availableSelectCols.isNotEmpty()) {
                    val commQuery = "SELECT ${availableSelectCols.joinToString(", ")} FROM $commentaryTableName"
                    val commCursor = sqliteDb.rawQuery(commQuery, null)

                    val commList = mutableListOf<BibleCommentaryEntity>()
                    if (commCursor.moveToFirst()) {
                        val bIdx = if (commCols.contains(bCol)) commCursor.getColumnIndex(bCol) else -1
                        val cfIdx = if (commCols.contains(cFromCol)) commCursor.getColumnIndex(cFromCol) else -1
                        val vfIdx = if (commCols.contains(vFromCol)) commCursor.getColumnIndex(vFromCol) else -1
                        val ctIdx = if (cToCol != null && commCols.contains(cToCol)) commCursor.getColumnIndex(cToCol) else -1
                        val vtIdx = if (vToCol != null && commCols.contains(vToCol)) commCursor.getColumnIndex(vToCol) else -1
                        val mIdx = if (markerCol != null && commCols.contains(markerCol)) commCursor.getColumnIndex(markerCol) else -1
                        val tIdx = if (commCols.contains(textColComm)) commCursor.getColumnIndex(textColComm) else -1

                        do {
                            val rawB = if (bIdx >= 0) commCursor.getInt(bIdx) else 1
                            val b = normalizeBookId(rawB)
                            val cf = if (cfIdx >= 0) commCursor.getInt(cfIdx) else 1
                            val vf = if (vfIdx >= 0) commCursor.getInt(vfIdx) else 1
                            val ct = if (ctIdx >= 0) commCursor.getInt(ctIdx) else 0
                            val vt = if (vtIdx >= 0) commCursor.getInt(vtIdx) else 0
                            val m = if (mIdx >= 0) commCursor.getString(mIdx) ?: "" else ""
                            val txt = if (tIdx >= 0) commCursor.getString(tIdx) ?: "" else ""
                            val cleanTxt = BibleLocalDataSource.decodeAndSanitizeVerseText(txt)

                            if (cleanTxt.isNotBlank()) {
                                commList.add(
                                    BibleCommentaryEntity(
                                        translationId = linkedTranslationId,
                                        bookId = b,
                                        chapterFrom = cf,
                                        verseFrom = vf,
                                        chapterTo = ct,
                                        verseTo = vt,
                                        marker = m,
                                        text = cleanTxt.trim()
                                    )
                                )
                            }

                            if (commList.size >= 1000) {
                                appDb.bibleDao().insertCommentaries(commList)
                                commList.clear()
                            }
                        } while (commCursor.moveToNext())
                    }
                    commCursor.close()

                    if (commList.isNotEmpty()) {
                        appDb.bibleDao().insertCommentaries(commList)
                    }
                }
            }

            // If it is solely a commentary file, finish cleanly
            if (isCommentaryFile && commentaryTableName != null) {
                sqliteDb.close()
                return@withContext Result.success(0)
            }

            // 2. Find verse table
            val ignoredTableNames = setOf(
                "info", "books", "stories", "introductions", "books_all",
                "android_metadata", "sqlite_sequence", "commentaries",
                "dictionary", "footnotes", "cross_references"
            )

            val candidateVerseTables = allTables.filter { tableName ->
                val lower = tableName.lowercase()
                !ignoredTableNames.contains(lower) && !lower.contains("commentar")
            }

            var chosenVerseTable: String? = null
            var chosenColNames: List<String> = emptyList()
            var chosenBookCol: String? = null
            var chosenChapterCol: String? = null
            var chosenVerseCol: String? = null
            var chosenTextCol: String? = null
            var chosenHeadingCol: String? = null

            // Prioritize tables with verse/bible/scripture, but verify columns
            val orderedCandidates = candidateVerseTables.sortedByDescending {
                val l = it.lowercase()
                when {
                    l.contains("verse") -> 3
                    l.contains("bible") || l.contains("scripture") -> 2
                    else -> 1
                }
            }

            for (t in orderedCandidates) {
                val colCursor = sqliteDb.rawQuery("PRAGMA table_info($t)", null)
                val cols = mutableListOf<String>()
                if (colCursor.moveToFirst()) {
                    do {
                        cols.add(colCursor.getString(1))
                    } while (colCursor.moveToNext())
                }
                colCursor.close()

                val bCol = cols.firstOrNull { it.equals("book", true) || it.equals("book_number", true) || it.equals("book_id", true) || it.equals("b", true) || it.equals("booknumber", true) }
                val cCol = cols.firstOrNull { it.equals("chapter", true) || it.equals("chapter_number", true) || it.equals("c", true) || it.equals("chap", true) || it.equals("chapternumber", true) }
                val vCol = cols.firstOrNull { it.equals("verse", true) || it.equals("verse_number", true) || it.equals("v", true) || it.equals("versenumber", true) }
                val tCol = cols.firstOrNull { it.equals("scripture", true) || it.equals("text", true) || it.equals("content", true) || it.equals("verse_text", true) || it.equals("t", true) || it.equals("words", true) || it.equals("body", true) }
                val hCol = cols.firstOrNull { it.equals("title", true) || it.equals("heading", true) || it.equals("sub_heading", true) || it.equals("subheading", true) || it.equals("h", true) || it.equals("header", true) }

                // Must have text column and at least one coordinate column (book/chapter/verse)
                if (tCol != null && (bCol != null || cCol != null || vCol != null)) {
                    chosenVerseTable = t
                    chosenColNames = cols
                    chosenBookCol = bCol ?: "book"
                    chosenChapterCol = cCol ?: "chapter"
                    chosenVerseCol = vCol ?: "verse"
                    chosenTextCol = tCol
                    chosenHeadingCol = hCol
                    break
                }
            }

            if (chosenVerseTable == null) {
                sqliteDb.close()
                return@withContext Result.success(0)
            }

            if (replaceExisting) {
                appDb.bibleDao().deleteVersesByTranslation(targetTranslationId)
            }

            var count = 0
            val selectCols = mutableListOf<String>()
            if (chosenColNames.contains(chosenBookCol)) selectCols.add(chosenBookCol!!)
            if (chosenColNames.contains(chosenChapterCol)) selectCols.add(chosenChapterCol!!)
            if (chosenColNames.contains(chosenVerseCol)) selectCols.add(chosenVerseCol!!)
            if (chosenColNames.contains(chosenTextCol)) selectCols.add(chosenTextCol!!)
            if (chosenHeadingCol != null && chosenColNames.contains(chosenHeadingCol) && !selectCols.contains(chosenHeadingCol)) {
                selectCols.add(chosenHeadingCol!!)
            }

            if (selectCols.isNotEmpty()) {
                val query = "SELECT ${selectCols.joinToString(", ")} FROM $chosenVerseTable"
                val verseCursor = sqliteDb.rawQuery(query, null)

                val versesToInsert = mutableListOf<BibleVerseEntity>()
                val headingsToInsert = mutableListOf<BibleHeadingEntity>()

                if (verseCursor.moveToFirst()) {
                    val bookIdx = if (chosenColNames.contains(chosenBookCol)) verseCursor.getColumnIndex(chosenBookCol) else -1
                    val chapIdx = if (chosenColNames.contains(chosenChapterCol)) verseCursor.getColumnIndex(chosenChapterCol) else -1
                    val verseIdx = if (chosenColNames.contains(chosenVerseCol)) verseCursor.getColumnIndex(chosenVerseCol) else -1
                    val textIdx = if (chosenColNames.contains(chosenTextCol)) verseCursor.getColumnIndex(chosenTextCol) else -1
                    val headingIdx = if (chosenHeadingCol != null && chosenColNames.contains(chosenHeadingCol)) verseCursor.getColumnIndex(chosenHeadingCol) else -1

                    do {
                        val rawB = if (bookIdx >= 0) verseCursor.getInt(bookIdx) else 1
                        val b = normalizeBookId(rawB)
                        val c = if (chapIdx >= 0) verseCursor.getInt(chapIdx) else 1
                        val v = if (verseIdx >= 0) verseCursor.getInt(verseIdx) else 1
                        val t = if (textIdx >= 0) verseCursor.getString(textIdx) ?: "" else ""
                        val h = if (headingIdx >= 0) verseCursor.getString(headingIdx) ?: "" else ""

                        if (v == 0) {
                            val headingRaw = if (h.isNotBlank()) h else t
                            val headingText = BibleLocalDataSource.decodeAndSanitizeVerseText(headingRaw)
                            if (headingText.isNotBlank()) {
                                headingsToInsert.add(
                                    BibleHeadingEntity(
                                        translationId = targetTranslationId,
                                        bookId = b,
                                        chapter = c,
                                        beforeVerse = 1,
                                        headingText = headingText.trim()
                                    )
                                )
                            }
                        } else {
                            if (h.isNotBlank()) {
                                val headingText = BibleLocalDataSource.decodeAndSanitizeVerseText(h)
                                if (headingText.isNotBlank()) {
                                    headingsToInsert.add(
                                        BibleHeadingEntity(
                                            translationId = targetTranslationId,
                                            bookId = b,
                                            chapter = c,
                                            beforeVerse = v,
                                            headingText = headingText.trim()
                                        )
                                    )
                                }
                            }
                            if (t.contains("<TS>", ignoreCase = true)) {
                                val tsMatches = Regex("<TS>(.*?)</?Ts>", RegexOption.IGNORE_CASE).findAll(t)
                                for (match in tsMatches) {
                                    val extractedHeading = BibleLocalDataSource.decodeAndSanitizeVerseText(match.groupValues[1])
                                    if (extractedHeading.isNotBlank()) {
                                        headingsToInsert.add(
                                            BibleHeadingEntity(
                                                translationId = targetTranslationId,
                                                bookId = b,
                                                chapter = c,
                                                beforeVerse = v,
                                                headingText = extractedHeading.trim()
                                            )
                                        )
                                    }
                                }
                            }
                            val cleanVerseText = BibleLocalDataSource.decodeAndSanitizeVerseText(t)
                            if (cleanVerseText.isNotBlank()) {
                                versesToInsert.add(
                                    BibleVerseEntity(
                                        translationId = targetTranslationId,
                                        bookId = b,
                                        chapter = c,
                                        verse = v,
                                        text = cleanVerseText.trim()
                                    )
                                )
                                count++
                            }
                        }

                        if (versesToInsert.size >= 1000) {
                            appDb.bibleDao().insertVerses(versesToInsert)
                            versesToInsert.clear()
                        }
                        if (headingsToInsert.size >= 1000) {
                            appDb.bibleDao().insertHeadings(headingsToInsert)
                            headingsToInsert.clear()
                        }
                    } while (verseCursor.moveToNext())
                }
                verseCursor.close()

                if (versesToInsert.isNotEmpty()) {
                    appDb.bibleDao().insertVerses(versesToInsert)
                }
                if (headingsToInsert.isNotEmpty()) {
                    appDb.bibleDao().insertHeadings(headingsToInsert)
                }
            }

            sqliteDb.close()
            Result.success(count)
        } catch (e: Exception) {
            try { sqliteDb?.close() } catch (ex: Exception) {}
            Result.failure(e)
        }
    }

    /**
     * Imports an SQLite3 database file from an InputStream / File Uri or Assets.
     * Flexibly inspects tables and columns (book, chapter, verse, text).
     */
    suspend fun importSqliteBibleFile(
        context: Context,
        appDb: BibleDatabase,
        sourceUri: Uri,
        targetTranslationId: String,
        replaceExisting: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "temp_imported_bible_${System.currentTimeMillis()}.db")
            if (tempFile.exists()) tempFile.delete()

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Failed to open input stream for SQLite file."))

            val result = importSqliteFileDirect(
                context = context,
                appDb = appDb,
                dbFile = tempFile,
                targetTranslationId = targetTranslationId,
                replaceExisting = replaceExisting
            )

            tempFile.delete()
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
