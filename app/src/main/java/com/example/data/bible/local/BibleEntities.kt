package com.example.data.bible.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bible_verses",
    primaryKeys = ["translationId", "bookId", "chapter", "verse"],
    indices = [
        Index(value = ["translationId", "bookId", "chapter"])
    ]
)
data class BibleVerseEntity(
    val translationId: String,
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val text: String
)

@Entity(
    tableName = "bible_commentaries",
    primaryKeys = ["translationId", "bookId", "chapterFrom", "verseFrom", "marker"],
    indices = [
        Index(value = ["translationId", "bookId", "chapterFrom", "verseFrom"])
    ]
)
data class BibleCommentaryEntity(
    val translationId: String,
    val bookId: Int,
    val chapterFrom: Int,
    val verseFrom: Int,
    val chapterTo: Int = 0,
    val verseTo: Int = 0,
    val marker: String = "",
    val text: String
)

@Entity(
    tableName = "bible_headings",
    primaryKeys = ["translationId", "bookId", "chapter", "beforeVerse"],
    indices = [
        Index(value = ["translationId", "bookId", "chapter"])
    ]
)
data class BibleHeadingEntity(
    val translationId: String,
    val bookId: Int,
    val chapter: Int,
    val beforeVerse: Int,
    val headingText: String
)

@Entity(
    tableName = "bible_bookmarks",
    indices = [Index(value = ["bookId", "chapter", "verse"], unique = true)]
)
data class BibleBookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Int,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val translationId: String,
    val verseText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bible_favorites",
    indices = [Index(value = ["bookId", "chapter", "verse"], unique = true)]
)
data class BibleFavoriteVerseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Int,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val translationId: String,
    val verseText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bible_highlights",
    indices = [Index(value = ["bookId", "chapter", "verse"], unique = true)]
)
data class BibleHighlightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val colorHex: String, // e.g. #FEF08A (yellow), #BAE6FD (blue), #BBF7D0 (green), #FBCFE8 (pink)
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bible_notes",
    indices = [Index(value = ["bookId", "chapter", "verse"], unique = true)]
)
data class BibleNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Int,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val noteText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_position")
data class ReadingPositionEntity(
    @PrimaryKey
    val id: Int = 1,
    val bookId: Int,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val translationId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reading_plan_progress",
    primaryKeys = ["planId", "dayNumber"]
)
data class ReadingPlanProgressEntity(
    val planId: String,
    val dayNumber: Int,
    val isCompleted: Boolean,
    val completedTimestamp: Long = 0L
)

@Entity(tableName = "StudyNotes")
data class StudyNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val noteId: Long = 0L,
    val title: String = "",
    val tags: String = "",
    val date: String = "",
    val time: String = "",
    val content: String = ""
)

@Entity(tableName = "christian_songs")
data class ChristianSongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songNumber: Int = 0,
    val title: String,
    val content: String,
    val artist: String = "Vinay Kumar AVJ",
    val category: String = "Hindi Worship",
    val keyScale: String = "D",
    val colorHex: String = "#FFFBEB",
    val textColorHex: String = "#1E293B",
    val linkedReferences: String = "",
    val personalNotes: String = "",
    val blogPostId: String = "",
    val isFavorite: Boolean = false,
    val isUserCreated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

