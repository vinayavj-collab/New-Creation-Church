package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.YouTubeVideoEntity
import com.example.data.model.PredefinedPlaylists
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.example.data.remote.YouTubeFeedService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

class YouTubeRepository(
    private val database: AppDatabase,
    private val feedService: YouTubeFeedService = YouTubeFeedService()
) {
    private val dao = database.youtubeVideoDao()
    private val playlistCache = ConcurrentHashMap<String, List<YouTubeVideo>>()

    private val _playlists = MutableStateFlow<List<YouTubePlaylist>>(PredefinedPlaylists.items)
    val playlistsFlow: Flow<List<YouTubePlaylist>> = _playlists.asStateFlow()

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

    suspend fun refreshPlaylists(): Result<List<YouTubePlaylist>> {
        return try {
            // Only fetch dynamic playlists for Worship Channel (@vinaykumaravjworship)
            val worshipPlaylists = feedService.fetchChannelPlaylists(
                channelHandle = PredefinedPlaylists.channelWorship.handle,
                channelId = PredefinedPlaylists.channelWorship.id,
                channelTitle = PredefinedPlaylists.channelWorship.name
            )

            // For @vinaykumaravj (Main channel), maintain fixed curated playlists only
            val mainFixedPlaylists = PredefinedPlaylists.items.filter {
                it.channelTitle != PredefinedPlaylists.channelWorship.name
            }

            val finalPlaylists = if (worshipPlaylists.isNotEmpty()) {
                (worshipPlaylists + mainFixedPlaylists).distinctBy { it.id }
            } else {
                PredefinedPlaylists.items
            }

            _playlists.value = finalPlaylists
            Result.success(finalPlaylists)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
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
            val churchVideos = feedService.fetchChannelVideos(
                PredefinedPlaylists.channelNewCreationChurch.id,
                PredefinedPlaylists.channelNewCreationChurch.name
            )

            val all = mainVideos + worshipVideos + churchVideos
            if (all.isNotEmpty()) {
                dao.insertVideos(all.map { YouTubeVideoEntity.fromDomain(it) })
            }

            // Also refresh playlists dynamically in background
            try {
                refreshPlaylists()
            } catch (e: Exception) {
                // Ignore playlist background error
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

    fun getPlaylists(): List<YouTubePlaylist> = _playlists.value

    suspend fun clearCache() {
        dao.clearAll()
        playlistCache.clear()
    }
}
