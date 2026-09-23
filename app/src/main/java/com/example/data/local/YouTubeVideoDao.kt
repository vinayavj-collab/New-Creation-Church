package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface YouTubeVideoDao {
    @Query("SELECT * FROM youtube_videos ORDER BY publishedTimestamp DESC")
    fun getAllVideos(): Flow<List<YouTubeVideoEntity>>

    @Query("SELECT * FROM youtube_videos WHERE channelId = :channelId ORDER BY publishedTimestamp DESC")
    fun getVideosByChannel(channelId: String): Flow<List<YouTubeVideoEntity>>

    @Query("SELECT * FROM youtube_videos WHERE id = :id LIMIT 1")
    fun getVideoById(id: String): Flow<YouTubeVideoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<YouTubeVideoEntity>)

    @Query("DELETE FROM youtube_videos WHERE id LIKE 'local_vid%'")
    suspend fun deleteDummyVideos()

    @Query("DELETE FROM youtube_videos")
    suspend fun clearAll()
}
