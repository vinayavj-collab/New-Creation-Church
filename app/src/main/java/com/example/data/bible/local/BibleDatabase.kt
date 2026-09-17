package com.example.data.bible.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BibleVerseEntity::class,
        BibleCommentaryEntity::class,
        BibleHeadingEntity::class,
        BibleBookmarkEntity::class,
        BibleFavoriteVerseEntity::class,
        BibleHighlightEntity::class,
        BibleNoteEntity::class,
        ReadingPositionEntity::class,
        ReadingPlanProgressEntity::class,
        StudyNoteEntity::class,
        ChristianSongEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun bibleDao(): BibleDao

    companion object {
        @Volatile
        private var INSTANCE: BibleDatabase? = null

        fun getInstance(context: Context): BibleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BibleDatabase::class.java,
                    "vinay_kumar_avj_bible.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
