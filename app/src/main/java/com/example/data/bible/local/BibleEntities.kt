package com.example.data.bible.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bible_verses",
    indices = [
        Index(value = ["translationId", "bookId", "chapter", "verse"], unique = true),
        Index(value = ["translationId", "bookId", "chapter"])
    ]
)
data class BibleVerseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val translationId: String,
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val text: String
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
