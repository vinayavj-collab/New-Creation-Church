package com.example.data.bible.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bible_verses", primaryKeys = ["translationId", "bookId", "chapter", "verseNumber"])
data class BibleVerseEntity(
    val translationId: String,
    val bookId: Int,
    val chapter: Int,
    val verseNumber: Int,
    val text: String,
    val bookName: String = ""
) {
    val verse: Int get() = verseNumber

    constructor(
        translationId: String,
        bookId: Int,
        chapter: Int,
        verse: Int,
        text: String
    ) : this(
        translationId = translationId,
        bookId = bookId,
        chapter = chapter,
        verseNumber = verse,
        text = text,
        bookName = ""
    )
}

@Entity(tableName = "bible_headings", primaryKeys = ["translationId", "bookId", "chapter", "beforeVerse"])
data class BibleHeadingEntity(
    val translationId: String,
    val bookId: Int,
    val chapter: Int,
    val beforeVerse: Int,
    val headingText: String
)

@Entity(tableName = "bible_commentaries", primaryKeys = ["translationId", "bookId", "chapterFrom", "verseFrom", "chapterTo", "verseTo", "marker"])
data class BibleCommentaryEntity(
    val translationId: String,
    val bookId: Int,
    val chapterFrom: Int,
    val verseFrom: Int,
    val chapterTo: Int,
    val verseTo: Int,
    val marker: String = "",
    val text: String = ""
) {
    val chapter: Int get() = chapterFrom
    val verse: Int get() = verseFrom
    val commentaryText: String get() = text
}

@Entity(tableName = "bible_bookmarks")
data class BibleBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Int = 0,
    val chapter: Int = 0,
    val verse: Int = 0,
    val bookName: String = "",
    val translationId: String = "HIOV",
    val verseText: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val timestamp: Long get() = createdAt

    constructor(bookId: Int, chapter: Int, verse: Int) : this(
        id = 0,
        bookId = bookId,
        chapter = chapter,
        verse = verse,
        bookName = "",
        translationId = "HIOV",
        verseText = "",
        createdAt = System.currentTimeMillis()
    )
}

@Entity(tableName = "bible_favorites")
data class BibleFavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Int = 0,
    val chapter: Int = 0,
    val verse: Int = 0,
    val bookName: String = "",
    val translationId: String = "HIOV",
    val verseText: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val timestamp: Long get() = createdAt

    constructor(bookId: Int, chapter: Int, verse: Int) : this(
        id = 0,
        bookId = bookId,
        chapter = chapter,
        verse = verse,
        bookName = "",
        translationId = "HIOV",
        verseText = "",
        createdAt = System.currentTimeMillis()
    )
}

typealias BibleFavoriteVerseEntity = BibleFavoriteEntity

@Entity(tableName = "bible_highlights", primaryKeys = ["bookId", "chapter", "verse"])
data class BibleHighlightEntity(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    val id: String get() = "${bookId}_${chapter}_$verse"
    val timestamp: Long get() = createdAt
}

@Entity(tableName = "bible_notes", primaryKeys = ["bookId", "chapter", "verse"])
data class BibleNoteEntity(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val noteText: String,
    val bookName: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val id: String get() = "${bookId}_${chapter}_$verse"
    val timestamp: Long get() = updatedAt
}

@Entity(tableName = "reading_position")
data class ReadingPositionEntity(
    @PrimaryKey val id: Int = 1,
    val bookId: Int,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val translationId: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "christian_songs")
data class ChristianSongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songNumber: Int = 0,
    val title: String = "",
    val content: String = "",
    val artist: String = "Christian Worship",
    val category: String = "स्तुति व आराधना",
    val keyScale: String = "D",
    val colorHex: String = "#FFFBEB",
    val textColorHex: String = "#000000",
    val linkedReferences: String = "",
    val personalNotes: String = "",
    val blogPostId: String? = null,
    val isFavorite: Boolean = false,
    val isUserCreated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_notes")
data class StudyNoteEntity(
    @PrimaryKey(autoGenerate = true) val noteId: Long = 0,
    val title: String = "",
    val tags: String = "",
    val date: String = "",
    val time: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_plan_progress", primaryKeys = ["planId", "dayNumber"])
data class ReadingPlanProgressEntity(
    val planId: String,
    val dayNumber: Int,
    val isCompleted: Boolean,
    val completedTimestamp: Long = 0L
)
