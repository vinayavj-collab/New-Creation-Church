package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.YouTubeVideoEntity
import com.example.data.model.PredefinedPlaylists
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.example.data.remote.YouTubeFeedService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

class YouTubeRepository(
    private val database: AppDatabase,
    private val feedService: YouTubeFeedService = YouTubeFeedService()
) {
    private val dao = database.youtubeVideoDao()
    private val playlistCache = ConcurrentHashMap<String, List<YouTubeVideo>>()

    fun getAllVideosFlow(): Flow<List<YouTubeVideo>> {
        return dao.getAllVideos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getVideosByChannelFlow(channelId: String): Flow<List<YouTubeVideo>> {
        return dao.getVideosByChannel(channelId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun refreshChannelVideos(): Result<Unit> {
        return try {
            val mainVideos = feedService.fetchChannelVideos(
                PredefinedPlaylists.channelMain.id,
                PredefinedPlaylists.channelMain.name
            )
            val worshipVideos = feedService.fetchChannelVideos(
                PredefinedPlaylists.channelWorship.id,
                PredefinedPlaylists.channelWorship.name
            )

            val all = mainVideos + worshipVideos
            if (all.isNotEmpty()) {
                dao.insertVideos(all.map { YouTubeVideoEntity.fromDomain(it) })
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylistVideos(playlist: YouTubePlaylist, forceRefresh: Boolean = false): List<YouTubeVideo> {
        if (!forceRefresh && playlistCache.containsKey(playlist.id)) {
            return playlistCache[playlist.id].orEmpty()
        }
        val videos = feedService.fetchPlaylistVideos(playlist.id, playlist.channelTitle)
        if (videos.isNotEmpty()) {
            playlistCache[playlist.id] = videos
        }
        return videos
    }

    fun getPlaylists(): List<YouTubePlaylist> = PredefinedPlaylists.items

    suspend fun clearCache() {
        dao.clearAll()
        playlistCache.clear()
    }
}
