package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.YouTubeVideo

@Entity(tableName = "youtube_videos")
data class YouTubeVideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val publishedAt: String,
    val publishedTimestamp: Long,
    val description: String,
    val videoUrl: String
) {
    fun toDomain(): YouTubeVideo {
        return YouTubeVideo(
            id = id,
            title = title,
            channelId = channelId,
            channelTitle = channelTitle,
            thumbnailUrl = thumbnailUrl,
            publishedAt = publishedAt,
            publishedTimestamp = publishedTimestamp,
            description = description,
            videoUrl = videoUrl
        )
    }

    companion object {
        fun fromDomain(video: YouTubeVideo): YouTubeVideoEntity {
            return YouTubeVideoEntity(
                id = video.id,
                title = video.title,
                channelId = video.channelId,
                channelTitle = video.channelTitle,
                thumbnailUrl = video.thumbnailUrl,
                publishedAt = video.publishedAt,
                publishedTimestamp = video.publishedTimestamp,
                description = video.description,
                videoUrl = video.videoUrl
            )
        }
    }
}
