package com.example.data.bible.repository

import com.example.data.bible.local.BibleDao
import com.example.data.bible.local.StudyNoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

class StudyNotesRepository(private val bibleDao: BibleDao) {

    fun getAllNotesByDate(): Flow<List<StudyNoteEntity>> = try {
        bibleDao.getAllStudyNotesByDate()
    } catch (e: Exception) {
        flowOf(emptyList())
    }

    fun getAllNotesByIdAsc(): Flow<List<StudyNoteEntity>> = try {
        bibleDao.getAllStudyNotesByIdAsc()
    } catch (e: Exception) {
        flowOf(emptyList())
    }

    fun searchNotes(query: String): Flow<List<StudyNoteEntity>> = try {
        bibleDao.searchStudyNotes(query)
    } catch (e: Exception) {
        flowOf(emptyList())
    }

    suspend fun getNoteById(id: Long): StudyNoteEntity? = withContext(Dispatchers.IO) {
        try {
            bibleDao.getStudyNoteById(id)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun insertNote(note: StudyNoteEntity): Long = withContext(Dispatchers.IO) {
        try {
            bibleDao.insertStudyNote(note)
        } catch (e: Exception) {
            -1L
        }
    }

    suspend fun updateNote(note: StudyNoteEntity) = withContext(Dispatchers.IO) {
        try {
            bibleDao.updateStudyNote(note)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getMostRecentNote(): StudyNoteEntity? = withContext(Dispatchers.IO) {
        try {
            bibleDao.getMostRecentStudyNote()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchScriptureText(
        bookId: Int,
        chapter: Int,
        startVerse: Int,
        endVerse: Int,
        translationId: String = "HIOV"
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val book = com.example.data.bible.model.BibleBookDefinitions.getBookById(bookId) ?: return@withContext null
            val effEnd = if (endVerse >= startVerse) endVerse else startVerse
            var verses = bibleDao.getVersesRangeSync(translationId, bookId, chapter, startVerse, effEnd)
            if (verses.isEmpty() && translationId != "HIOV") {
                verses = bibleDao.getVersesRangeSync("HIOV", bookId, chapter, startVerse, effEnd)
            }
            if (verses.isEmpty()) {
                val chVerses = bibleDao.getVersesForChapterSync(translationId, bookId, chapter)
                    .ifEmpty { bibleDao.getVersesForChapterSync("HIOV", bookId, chapter) }
                verses = chVerses.filter { it.verse in startVerse..effEnd }.sortedBy { it.verse }
            }
            if (verses.isEmpty()) return@withContext null

            val refLabel = if (startVerse == effEnd) {
                "${book.nameHindi} $chapter:$startVerse ($translationId)"
            } else {
                "${book.nameHindi} $chapter:$startVerse-$effEnd ($translationId)"
            }
            val text = verses.sortedBy { it.verse }.joinToString(" ") { entity ->
                val clean = com.example.data.bible.local.BibleLocalDataSource.decodeAndSanitizeVerseText(entity.text)
                if (verses.size > 1) "(${entity.verse}) $clean" else clean
            }
            Pair(refLabel, text)
        } catch (e: Exception) {
            null
        }
    }

    fun formatScriptureHtmlBlock(referenceLabel: String, scriptureText: String): String {
        return """<div style="border: 1px dashed #78909c; background-color: #f8fafc; color: #1e293b; padding: 12px; margin: 10px 0; border-radius: 8px;"><strong>📖 $referenceLabel</strong><br/><span style="font-size: 14px; line-height: 1.5; color: #334155;">$scriptureText</span></div><br/>"""
    }

    suspend fun appendScriptureToRecentNote(
        referenceLabel: String,
        scriptureText: String
    ): Pair<Long, String> = withContext(Dispatchers.IO) {
        val htmlBlock = formatScriptureHtmlBlock(referenceLabel, scriptureText)
        val recentNote = getMostRecentNote()
        val now = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val timeNow = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())

        if (recentNote != null) {
            val updatedContent = if (recentNote.content.isBlank()) {
                htmlBlock
            } else {
                "${recentNote.content}<br/>$htmlBlock"
            }
            val updatedNote = recentNote.copy(
                content = updatedContent,
                date = now,
                time = timeNow
            )
            updateNote(updatedNote)
            Pair(recentNote.noteId, recentNote.title)
        } else {
            val newNote = StudyNoteEntity(
                title = "Study Note - $referenceLabel",
                tags = "SN#01, Scripture",
                date = now,
                time = timeNow,
                content = htmlBlock
            )
            val newId = insertNote(newNote)
            Pair(newId, newNote.title)
        }
    }

    suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) {
        try {
            bibleDao.deleteStudyNoteById(id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
