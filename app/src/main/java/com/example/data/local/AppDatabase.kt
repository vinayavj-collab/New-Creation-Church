package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BlogPostEntity::class,
        YouTubeVideoEntity::class,
        SavedItemEntity::class,
        RecentlyViewedEntity::class,
        NotificationEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blogPostDao(): BlogPostDao
    abstract fun youtubeVideoDao(): YouTubeVideoDao
    abstract fun savedItemDao(): SavedItemDao
    abstract fun recentlyViewedDao(): RecentlyViewedDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vinay_kumar_avj_hub.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
