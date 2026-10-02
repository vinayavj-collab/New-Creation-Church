package com.example.data.bible.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BibleVerseEntity::class,
        BibleHeadingEntity::class,
        BibleCommentaryEntity::class,
        BibleBookmarkEntity::class,
        BibleFavoriteEntity::class,
        BibleHighlightEntity::class,
        BibleNoteEntity::class,
        ReadingPositionEntity::class,
        ChristianSongEntity::class,
        StudyNoteEntity::class,
        ReadingPlanProgressEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun bibleDao(): BibleDao

    companion object {
        @Volatile
        private var INSTANCE: BibleDatabase? = null

        fun getDatabase(context: Context): BibleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BibleDatabase::class.java,
                    "bible_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): BibleDatabase = getDatabase(context)
    }
}
