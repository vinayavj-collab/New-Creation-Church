package com.example.data.bible.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleDao {
    @Query("""
        SELECT * FROM bible_verses 
        WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter 
        ORDER BY verse ASC
    """)
    fun getVersesForChapter(translationId: String, bookId: Int, chapter: Int): Flow<List<BibleVerseEntity>>

    @Query("""
        SELECT * FROM bible_verses 
        WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter 
        ORDER BY verse ASC
    """)
    suspend fun getVersesForChapterSync(translationId: String, bookId: Int, chapter: Int): List<BibleVerseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<BibleVerseEntity>)

    // Commentaries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommentaries(commentaries: List<BibleCommentaryEntity>)

    @Query("DELETE FROM bible_commentaries")
    suspend fun clearAllCommentaries()

    @Query("DELETE FROM bible_commentaries WHERE translationId = :translationId")
    suspend fun deleteCommentariesByTranslation(translationId: String)

    @Query("""
        SELECT * FROM bible_commentaries 
        WHERE (translationId = :translationId OR translationId = :translationId || '_commentaries' OR translationId = 'HIOV' OR translationId = 'HIOV_commentaries')
          AND bookId = :bookId 
          AND chapterFrom = :chapter 
          AND verseFrom = :verse
    """)
    fun getCommentariesForVerse(translationId: String, bookId: Int, chapter: Int, verse: Int): Flow<List<BibleCommentaryEntity>>

    @Query("""
        SELECT * FROM bible_commentaries 
        WHERE (translationId = :translationId OR translationId = :translationId || '_commentaries' OR translationId = 'HIOV' OR translationId = 'HIOV_commentaries')
          AND bookId = :bookId 
          AND chapterFrom = :chapter
    """)
    fun getCommentariesForChapter(translationId: String, bookId: Int, chapter: Int): Flow<List<BibleCommentaryEntity>>

    @Query("DELETE FROM bible_verses")
    suspend fun clearAllVerses()

    @Query("DELETE FROM bible_verses WHERE translationId = :translationId")
    suspend fun deleteVersesByTranslation(translationId: String)

    @Query("DELETE FROM bible_verses WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter")
    suspend fun deleteChapterVerses(translationId: String, bookId: Int, chapter: Int)

    @Transaction
    suspend fun replaceChapterVerses(translationId: String, bookId: Int, chapter: Int, verses: List<BibleVerseEntity>) {
        deleteChapterVerses(translationId, bookId, chapter)
        insertVerses(verses)
    }

    @Query("""
        SELECT * FROM bible_verses 
        WHERE translationId = :translationId AND text LIKE '%' || :query || '%' 
        ORDER BY bookId ASC, chapter ASC, verse ASC 
        LIMIT 300
    """)
    fun searchVerses(translationId: String, query: String): Flow<List<BibleVerseEntity>>

    @Query("SELECT COUNT(*) FROM bible_verses WHERE translationId = :translationId")
    suspend fun getVerseCount(translationId: String): Int

    // Headings
    @Query("""
        SELECT * FROM bible_headings 
        WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter 
        ORDER BY beforeVerse ASC
    """)
    fun getHeadingsForChapter(translationId: String, bookId: Int, chapter: Int): Flow<List<BibleHeadingEntity>>

    @Query("""
        SELECT * FROM bible_headings 
        WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter 
        ORDER BY beforeVerse ASC
    """)
    suspend fun getHeadingsForChapterSync(translationId: String, bookId: Int, chapter: Int): List<BibleHeadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeadings(headings: List<BibleHeadingEntity>)

    @Query("SELECT COUNT(*) FROM bible_headings WHERE translationId = :translationId")
    suspend fun getHeadingCount(translationId: String): Int

    @Query("DELETE FROM bible_headings WHERE translationId = :translationId")
    suspend fun deleteHeadingsByTranslation(translationId: String)

    // Bookmarks
    @Query("SELECT * FROM bible_bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BibleBookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bible_bookmarks WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse)")
    fun isVerseBookmarked(bookId: Int, chapter: Int, verse: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BibleBookmarkEntity)

    @Query("DELETE FROM bible_bookmarks WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun deleteBookmark(bookId: Int, chapter: Int, verse: Int)

    @Query("DELETE FROM bible_bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    // Favorites
    @Query("SELECT * FROM bible_favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<BibleFavoriteVerseEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bible_favorites WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse)")
    fun isVerseFavorite(bookId: Int, chapter: Int, verse: Int): Flow<Boolean>

    @Query("SELECT * FROM bible_favorites WHERE bookId = :bookId AND chapter = :chapter")
    fun getFavoritesForChapter(bookId: Int, chapter: Int): Flow<List<BibleFavoriteVerseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: BibleFavoriteVerseEntity)

    @Query("DELETE FROM bible_favorites WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun deleteFavorite(bookId: Int, chapter: Int, verse: Int)

    @Query("DELETE FROM bible_favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)

    // Highlights
    @Query("SELECT * FROM bible_highlights WHERE bookId = :bookId AND chapter = :chapter")
    fun getHighlightsForChapter(bookId: Int, chapter: Int): Flow<List<BibleHighlightEntity>>

    @Query("SELECT * FROM bible_highlights ORDER BY timestamp DESC")
    fun getAllHighlights(): Flow<List<BibleHighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setHighlight(highlight: BibleHighlightEntity)

    @Query("DELETE FROM bible_highlights WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun removeHighlight(bookId: Int, chapter: Int, verse: Int)

    // Notes
    @Query("SELECT * FROM bible_notes WHERE bookId = :bookId AND chapter = :chapter")
    fun getNotesForChapter(bookId: Int, chapter: Int): Flow<List<BibleNoteEntity>>

    @Query("SELECT * FROM bible_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<BibleNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNote(note: BibleNoteEntity)

    @Query("DELETE FROM bible_notes WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun deleteNote(bookId: Int, chapter: Int, verse: Int)

    // Reading Position
    @Query("SELECT * FROM reading_position WHERE id = 1 LIMIT 1")
    fun getReadingPosition(): Flow<ReadingPositionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReadingPosition(position: ReadingPositionEntity)

    // Reading Plan Progress
    @Query("SELECT * FROM reading_plan_progress WHERE planId = :planId")
    fun getPlanProgress(planId: String): Flow<List<ReadingPlanProgressEntity>>

    @Query("SELECT * FROM reading_plan_progress")
    fun getAllProgress(): Flow<List<ReadingPlanProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPlanDayCompleted(progress: ReadingPlanProgressEntity)

    @Query("DELETE FROM reading_plan_progress WHERE planId = :planId")
    suspend fun resetPlan(planId: String)

    @Query("""
        SELECT * FROM bible_verses 
        WHERE translationId = :translationId AND bookId = :bookId AND chapter = :chapter AND verse >= :fromVerse AND verse <= :toVerse 
        ORDER BY verse ASC
    """)
    suspend fun getVersesRangeSync(translationId: String, bookId: Int, chapter: Int, fromVerse: Int, toVerse: Int): List<BibleVerseEntity>

    @Query("SELECT * FROM StudyNotes ORDER BY date DESC, noteId DESC LIMIT 1")
    suspend fun getMostRecentStudyNote(): StudyNoteEntity?

    // Study Notes
    @Query("SELECT * FROM StudyNotes ORDER BY date DESC, noteId DESC")
    fun getAllStudyNotesByDate(): Flow<List<StudyNoteEntity>>

    @Query("SELECT * FROM StudyNotes ORDER BY noteId ASC")
    fun getAllStudyNotesByIdAsc(): Flow<List<StudyNoteEntity>>

    @Query("SELECT * FROM StudyNotes WHERE noteId = :id LIMIT 1")
    suspend fun getStudyNoteById(id: Long): StudyNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyNote(note: StudyNoteEntity): Long

    @Update
    suspend fun updateStudyNote(note: StudyNoteEntity)

    @Query("DELETE FROM StudyNotes WHERE noteId = :id")
    suspend fun deleteStudyNoteById(id: Long)

    @Query("SELECT * FROM StudyNotes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchStudyNotes(query: String): Flow<List<StudyNoteEntity>>

    // Christian Songs / Song Book
    @Query("SELECT * FROM christian_songs ORDER BY CASE WHEN songNumber > 0 THEN songNumber ELSE 99999 END ASC, title ASC")
    fun getAllSongs(): Flow<List<ChristianSongEntity>>

    @Query("SELECT * FROM christian_songs WHERE isFavorite = 1 ORDER BY CASE WHEN songNumber > 0 THEN songNumber ELSE 99999 END ASC, title ASC")
    fun getFavoriteSongs(): Flow<List<ChristianSongEntity>>

    @Query("SELECT * FROM christian_songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): ChristianSongEntity?

    @Query("SELECT * FROM christian_songs WHERE songNumber = :number LIMIT 1")
    suspend fun getSongByNumber(number: Int): ChristianSongEntity?

    @Query("SELECT * FROM christian_songs WHERE LOWER(TRIM(title)) = LOWER(TRIM(:title)) LIMIT 1")
    suspend fun getSongByTitle(title: String): ChristianSongEntity?

    @Query("UPDATE christian_songs SET isFavorite = :isFavorite, modifiedAt = :timestamp WHERE id = :id")
    suspend fun setSongFavorite(id: Long, isFavorite: Boolean, timestamp: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: ChristianSongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<ChristianSongEntity>)

    @Update
    suspend fun updateSong(song: ChristianSongEntity)

    @Query("DELETE FROM christian_songs WHERE id = :id")
    suspend fun deleteSongById(id: Long)

    @Query("SELECT * FROM christian_songs WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR CAST(songNumber AS TEXT) = :query")
    fun searchSongs(query: String): Flow<List<ChristianSongEntity>>

    @Query("SELECT MAX(songNumber) FROM christian_songs")
    suspend fun getMaxSongNumber(): Int?

    @Query("DELETE FROM christian_songs WHERE isUserCreated = 0")
    suspend fun deleteAllNonCustomSongs()

    @Query("DELETE FROM christian_songs")
    suspend fun clearAllSongs()

    @Query("SELECT COUNT(*) FROM christian_songs")
    suspend fun getSongCount(): Int
}

