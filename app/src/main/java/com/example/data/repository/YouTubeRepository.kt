package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.PredefinedData
import com.example.data.local.YouTubeVideoEntity
import com.example.data.model.PredefinedPlaylists
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.example.data.remote.DailymotionFeedService
import com.example.data.remote.DailymotionPaginatedResult
import com.example.data.remote.YouTubeFeedService
import com.example.data.remote.YouTubePaginatedResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

class YouTubeRepository(
    private val database: AppDatabase,
    private val feedService: YouTubeFeedService = YouTubeFeedService(),
    private val dailymotionService: DailymotionFeedService = DailymotionFeedService()
) {
    private val dao = database.youtubeVideoDao()
    private val firebaseDataRepository by lazy { FirebaseDataRepository.getInstance() }
    private val playlistCache = ConcurrentHashMap<String, List<YouTubeVideo>>()

    private val _playlists = MutableStateFlow<List<YouTubePlaylist>>(PredefinedPlaylists.items)
    val playlistsFlow: Flow<List<YouTubePlaylist>> = combine(_playlists, firebaseDataRepository.playlists) { localPlaylists, remotePlaylists ->
        FirebaseDataRepository.mergeAndDeduplicate(
            remoteItems = remotePlaylists,
            localHardcodedItems = localPlaylists,
            keySelector = { it.id.ifBlank { it.playlistUrl } }
        )
    }

    fun getAllVideosFlow(): Flow<List<YouTubeVideo>> {
        return combine(dao.getAllVideos(), firebaseDataRepository.videos) { entities, remoteMerged ->
            val dbVideos = entities.map { it.toDomain() }
            val merged = FirebaseDataRepository.mergeAndDeduplicate(
                remoteItems = remoteMerged,
                localHardcodedItems = PredefinedData.hardcodedVideos + dbVideos,
                keySelector = { it.id.ifBlank { it.videoUrl } },
                timestampSelector = { it.publishedTimestamp }
            )
            merged.filter { candidate ->
                !candidate.id.startsWith("local_vid") &&
                !candidate.id.startsWith("dm_x27lzjr_") &&
                !candidate.id.startsWith("dm_x4sr8o4_") &&
                !candidate.thumbnailUrl.contains("local_vid")
            }
        }
    }

    fun getVideosByChannelFlow(channelId: String): Flow<List<YouTubeVideo>> {
        return combine(dao.getVideosByChannel(channelId), firebaseDataRepository.videos) { entities, remoteMerged ->
            val dbVideos = entities.map { it.toDomain() }
            val matchingRemote = remoteMerged.filter { it.channelId == channelId }
            val matchingHardcoded = PredefinedData.hardcodedVideos.filter { it.channelId == channelId }
            val merged = FirebaseDataRepository.mergeAndDeduplicate(
                remoteItems = matchingRemote,
                localHardcodedItems = matchingHardcoded + dbVideos,
                keySelector = { it.id.ifBlank { it.videoUrl } },
                timestampSelector = { it.publishedTimestamp }
            )
            merged.filter { candidate ->
                !candidate.id.startsWith("local_vid") &&
                !candidate.id.startsWith("dm_x27lzjr_") &&
                !candidate.id.startsWith("dm_x4sr8o4_") &&
                !candidate.thumbnailUrl.contains("local_vid")
            }
        }
    }

    suspend fun refreshPlaylists(): Result<List<YouTubePlaylist>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
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

            val fetchedPlaylists = if (worshipPlaylists.isNotEmpty()) {
                (worshipPlaylists + mainFixedPlaylists).distinctBy { it.id }
            } else {
                PredefinedPlaylists.items
            }

            val finalPlaylists = FirebaseDataRepository.mergeAndDeduplicate(
                remoteItems = firebaseDataRepository.playlists.value,
                localHardcodedItems = fetchedPlaylists,
                keySelector = { it.id.ifBlank { it.playlistUrl } }
            )

            _playlists.value = finalPlaylists
            Result.success(finalPlaylists)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun refreshChannelVideos(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
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

            // 1. Christian Dailymotion Channel (x27lzjr) - Always fetched for all users
            val dmChristian = try {
                dailymotionService.fetchChristianVideos(page = 1, limit = 15).videos
            } catch (e: Exception) {
                emptyList()
            }

            // 2. Personal Vlog Dailymotion Channel (x4sr8o4) - Fetched and tagged only if server flag allows
            val dmVlog = if (com.example.util.RemoteConfigHelper.isVlogServerEnabled()) {
                try {
                    dailymotionService.fetchPersonalVlogVideos(page = 1, limit = 15).videos
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            val all = mainVideos + worshipVideos + churchVideos + dmChristian + dmVlog
            dao.deleteDummyVideos()
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

    suspend fun getPlaylistVideos(playlist: YouTubePlaylist, forceRefresh: Boolean = false): List<YouTubeVideo> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!forceRefresh && playlistCache.containsKey(playlist.id)) {
            return@withContext playlistCache[playlist.id].orEmpty()
        }
        val videos = feedService.fetchPlaylistVideos(playlist.id, playlist.channelTitle)
        if (videos.isNotEmpty()) {
            playlistCache[playlist.id] = videos
        }
        videos
    }

    suspend fun fetchDailymotionPaginated(
        page: Int,
        limit: Int = 15,
        includePersonalVlog: Boolean = false
    ): DailymotionPaginatedResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // 1. Fetch Christian Channel (x27lzjr)
            val christianResult = dailymotionService.fetchChristianVideos(page = page, limit = limit)
            val combinedList = mutableListOf<YouTubeVideo>()
            combinedList.addAll(christianResult.videos)

            // 2. If Personal Vlog is enabled, fetch Personal Vlog Channel (x4sr8o4)
            if (includePersonalVlog) {
                val vlogResult = dailymotionService.fetchPersonalVlogVideos(page = page, limit = limit)
                combinedList.addAll(vlogResult.videos)
            }

            if (combinedList.isNotEmpty()) {
                dao.insertVideos(combinedList.map { YouTubeVideoEntity.fromDomain(it) })
            }

            DailymotionPaginatedResult(
                videos = combinedList,
                page = page,
                limit = limit,
                hasMore = christianResult.hasMore || combinedList.isNotEmpty()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            DailymotionPaginatedResult(emptyList(), page, limit, hasMore = false)
        }
    }

    suspend fun fetchMoreChannelVideos(
        channelId: String,
        channelTitle: String,
        pageToken: String?
    ): YouTubePaginatedResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val result = feedService.fetchChannelVideosPaginated(channelId, channelTitle, pageToken)
            if (result.videos.isNotEmpty()) {
                dao.insertVideos(result.videos.map { YouTubeVideoEntity.fromDomain(it) })
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            YouTubePaginatedResult(emptyList(), null, false)
        }
    }

    fun getPlaylists(): List<YouTubePlaylist> = _playlists.value

    suspend fun clearCache() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        dao.clearAll()
        playlistCache.clear()
    }
}
