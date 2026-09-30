package com.example.data.bible.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleDao {
    @Query("SELECT * FROM bible_verses WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter ORDER BY verseNumber ASC")
    fun getVersesForChapter(translationId: String, bookId: Int, chapter: Int): Flow<List<BibleVerseEntity>>

    @Query("SELECT * FROM bible_verses WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter ORDER BY verseNumber ASC")
    suspend fun getVersesForChapterSync(translationId: String, bookId: Int, chapter: Int): List<BibleVerseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<BibleVerseEntity>)

    @Query("SELECT * FROM bible_bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BibleBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BibleBookmarkEntity)

    @Query("DELETE FROM bible_bookmarks WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun deleteBookmark(bookId: Int, chapter: Int, verse: Int)

    @Query("SELECT * FROM bible_favorites ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<BibleFavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: BibleFavoriteEntity)

    @Query("DELETE FROM bible_favorites WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun deleteFavorite(bookId: Int, chapter: Int, verse: Int)

    @Query("SELECT * FROM bible_highlights WHERE bookId = :bookId AND chapter = :chapter")
    fun getHighlightsForChapter(bookId: Int, chapter: Int): Flow<List<BibleHighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: BibleHighlightEntity)

    @Query("DELETE FROM bible_highlights WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun deleteHighlight(bookId: Int, chapter: Int, verse: Int)

    @Query("SELECT * FROM bible_notes WHERE bookId = :bookId AND chapter = :chapter")
    fun getNotesForChapter(bookId: Int, chapter: Int): Flow<List<BibleNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: BibleNoteEntity)

    @Query("DELETE FROM bible_notes WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun deleteNote(bookId: Int, chapter: Int, verse: Int)
}
