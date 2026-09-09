package com.example.data.bible.repository

import com.example.data.bible.local.BibleDao
import com.example.data.bible.local.DedicatedNoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DedicatedNotesRepository(private val bibleDao: BibleDao) {

    fun getAllNotes(): Flow<List<DedicatedNoteEntity>> = bibleDao.getAllDedicatedNotes()

    fun searchNotes(query: String): Flow<List<DedicatedNoteEntity>> = bibleDao.searchDedicatedNotes(query)

    suspend fun getNoteById(id: Long): DedicatedNoteEntity? = withContext(Dispatchers.IO) {
        bibleDao.getDedicatedNoteById(id)
    }

    suspend fun insertNote(note: DedicatedNoteEntity): Long = withContext(Dispatchers.IO) {
        bibleDao.insertDedicatedNote(note)
    }

    suspend fun updateNote(note: DedicatedNoteEntity) = withContext(Dispatchers.IO) {
        bibleDao.updateDedicatedNote(note.copy(modifiedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) {
        bibleDao.deleteDedicatedNoteById(id)
    }
}
