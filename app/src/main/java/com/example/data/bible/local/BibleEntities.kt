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
)

@Entity(tableName = "bible_bookmarks", primaryKeys = ["bookId", "chapter", "verse"])
data class BibleBookmarkEntity(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bible_favorites", primaryKeys = ["bookId", "chapter", "verse"])
data class BibleFavoriteEntity(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bible_highlights", primaryKeys = ["bookId", "chapter", "verse"])
data class BibleHighlightEntity(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bible_notes", primaryKeys = ["bookId", "chapter", "verse"])
data class BibleNoteEntity(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val noteText: String,
    val updatedAt: Long = System.currentTimeMillis()
)
