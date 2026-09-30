package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.PredefinedData
import com.example.data.local.YouTubeVideoEntity
import com.example.data.model.PredefinedPlaylists
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.example.data.remote.DailymotionFeedService
import com.example.data.remote.YouTubeFeedService
import com.example.data.remote.YouTubePaginatedResult
import com.example.util.PersonalVlogSecurity
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

    private val _playlists = MutableStateFlow<List<YouTubePlaylist>>(
        PredefinedPlaylists.items.filter { !PredefinedPlaylists.isBlockedYouTubeChannel(it.channelTitle) }
    )
    val playlistsFlow: Flow<List<YouTubePlaylist>> = combine(_playlists, firebaseDataRepository.playlists) { localPlaylists, remotePlaylists ->
        val merged = FirebaseDataRepository.mergeAndDeduplicate(
            remoteItems = remotePlaylists,
            localHardcodedItems = localPlaylists,
            keySelector = { it.id.ifBlank { it.playlistUrl } }
        )
        merged.filter { pl ->
            !PredefinedPlaylists.isBlockedYouTubeChannel(pl.channelTitle) &&
            !PredefinedPlaylists.isBlockedYouTubeChannel(pl.title)
        }
    }

    fun getAllVideosFlow(): Flow<List<YouTubeVideo>> {
        return combine(
            dao.getAllVideos(),
            firebaseDataRepository.videos,
            com.example.util.ProfileManager.activeProfileFlow,
            firebaseDataRepository.pinnedVideoId
        ) { entities, remoteMerged, activeProfile, pinnedId ->
            val dbVideos = entities.map { it.toDomain() }
            val merged = FirebaseDataRepository.mergeAndDeduplicate(
                remoteItems = remoteMerged,
                localHardcodedItems = PredefinedData.hardcodedVideos + dbVideos,
                keySelector = { it.id.ifBlank { it.videoUrl } },
                timestampSelector = { it.publishedTimestamp }
            )
            val isVlogAllowed = (activeProfile == com.example.data.model.AppProfile.VINAY) || PersonalVlogSecurity.isPersonalVlogAllowed()
            merged.filter { candidate ->
                val titleLower = candidate.title.lowercase()
                val descLower = candidate.description.lowercase()
                val isVlogItem = candidate.id.startsWith("dm_${DailymotionFeedService.CHANNEL_VLOG_ID}_") ||
                        candidate.channelId == DailymotionFeedService.CHANNEL_VLOG_ID ||
                        candidate.channelTitle.contains("Vinay AVJ Vlog", ignoreCase = true) ||
                        candidate.channelTitle.contains("Vinay avj vlogs", ignoreCase = true) ||
                        (candidate.id.startsWith("dm_") && (candidate.channelTitle.contains("vlog", ignoreCase = true) || candidate.title.contains("vlog", ignoreCase = true)))
                !candidate.id.startsWith("local_vid") &&
                !candidate.thumbnailUrl.contains("local_vid") &&
                (!isVlogItem || isVlogAllowed) &&
                !titleLower.contains("metdaan") && !titleLower.contains("met daan") &&
                !descLower.contains("metdaan") && !descLower.contains("met daan") &&
                !PredefinedPlaylists.isBlockedYouTubeChannel(candidate.channelTitle, candidate.channelId)
            }.map { vid ->
                if (!pinnedId.isNullOrBlank() && (vid.id == pinnedId || vid.videoUrl.contains(pinnedId))) {
                    vid.copy(isPinned = true)
                } else {
                    vid.copy(isPinned = false)
                }
            }.sortedWith(
                compareByDescending<YouTubeVideo> { it.isPinned }
                    .thenByDescending { it.publishedTimestamp }
            )
        }
    }

    fun getVideosByChannelFlow(channelId: String): Flow<List<YouTubeVideo>> {
        return combine(
            dao.getVideosByChannel(channelId),
            firebaseDataRepository.videos,
            com.example.util.ProfileManager.activeProfileFlow,
            firebaseDataRepository.pinnedVideoId
        ) { entities, remoteMerged, activeProfile, pinnedId ->
            val dbVideos = entities.map { it.toDomain() }
            val matchingRemote = remoteMerged.filter { it.channelId == channelId }
            val matchingHardcoded = PredefinedData.hardcodedVideos.filter { it.channelId == channelId }
            val merged = FirebaseDataRepository.mergeAndDeduplicate(
                remoteItems = matchingRemote,
                localHardcodedItems = matchingHardcoded + dbVideos,
                keySelector = { it.id.ifBlank { it.videoUrl } },
                timestampSelector = { it.publishedTimestamp }
            )
            val isVlogAllowed = (activeProfile == com.example.data.model.AppProfile.VINAY) || PersonalVlogSecurity.isPersonalVlogAllowed()
            merged.filter { candidate ->
                val titleLower = candidate.title.lowercase()
                val descLower = candidate.description.lowercase()
                val isVlogItem = candidate.id.startsWith("dm_${DailymotionFeedService.CHANNEL_VLOG_ID}_") ||
                        candidate.channelId == DailymotionFeedService.CHANNEL_VLOG_ID ||
                        candidate.channelTitle.contains("Vinay AVJ Vlog", ignoreCase = true) ||
                        candidate.channelTitle.contains("Vinay avj vlogs", ignoreCase = true) ||
                        (candidate.id.startsWith("dm_") && (candidate.channelTitle.contains("vlog", ignoreCase = true) || candidate.title.contains("vlog", ignoreCase = true)))
                !candidate.id.startsWith("local_vid") &&
                !candidate.thumbnailUrl.contains("local_vid") &&
                (!isVlogItem || isVlogAllowed) &&
                !titleLower.contains("metdaan") && !titleLower.contains("met daan") &&
                !descLower.contains("metdaan") && !descLower.contains("met daan") &&
                !PredefinedPlaylists.isBlockedYouTubeChannel(candidate.channelTitle, candidate.channelId)
            }.map { vid ->
                if (!pinnedId.isNullOrBlank() && (vid.id == pinnedId || vid.videoUrl.contains(pinnedId))) {
                    vid.copy(isPinned = true)
                } else {
                    vid.copy(isPinned = false)
                }
            }.sortedWith(
                compareByDescending<YouTubeVideo> { it.isPinned }
                    .thenByDescending { it.publishedTimestamp }
            )
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

            // Fetch Dailymotion videos
            val dmMainVideos = try {
                dailymotionService.fetchUserVideos(
                    DailymotionFeedService.CHANNEL_MAIN_ID,
                    DailymotionFeedService.CHANNEL_MAIN_TITLE
                )
            } catch (e: Exception) {
                emptyList()
            }

            val dmVlogVideos = try {
                dailymotionService.fetchUserVideos(
                    DailymotionFeedService.CHANNEL_VLOG_ID,
                    DailymotionFeedService.CHANNEL_VLOG_TITLE
                )
            } catch (e: Exception) {
                emptyList()
            }

            val all = mainVideos + worshipVideos + churchVideos + dmMainVideos + dmVlogVideos
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

        // If this is a custom playlist with explicit videoUrls or videoIds
        if (playlist.videoUrls.isNotEmpty() || playlist.videoIds.isNotEmpty() || playlist.isCustom) {
            val customVideosList = mutableListOf<YouTubeVideo>()
            val allLocal = PredefinedData.hardcodedVideos + dao.getAllVideosList().map { it.toDomain() }
            
            // Map video URLs
            playlist.videoUrls.forEachIndexed { idx, url ->
                val parsed = com.example.util.VideoUrlParser.parse(url)
                val vidId = if (parsed.videoId.isNotBlank()) parsed.videoId else "vid_${playlist.id}_$idx"
                val existing = allLocal.find { it.id == vidId || it.videoUrl == url }
                val video = existing ?: YouTubeVideo(
                    id = vidId,
                    title = "${playlist.title} - Video #${idx + 1}",
                    channelId = "custom_channel",
                    channelTitle = playlist.channelTitle,
                    thumbnailUrl = if (parsed.thumbnailUrl.isNotBlank()) parsed.thumbnailUrl else "https://img.youtube.com/vi/$vidId/hqdefault.jpg",
                    publishedAt = "",
                    publishedTimestamp = System.currentTimeMillis() - (idx * 60000L),
                    description = playlist.description,
                    videoUrl = url,
                    isRemote = true
                )
                customVideosList.add(video)
            }

            // Map video IDs
            playlist.videoIds.forEachIndexed { idx, id ->
                if (customVideosList.none { it.id == id }) {
                    val existing = allLocal.find { it.id == id }
                    val video = existing ?: YouTubeVideo(
                        id = id,
                        title = "${playlist.title} - Part ${idx + 1}",
                        channelId = "custom_channel",
                        channelTitle = playlist.channelTitle,
                        thumbnailUrl = "https://img.youtube.com/vi/$id/hqdefault.jpg",
                        publishedAt = "",
                        publishedTimestamp = System.currentTimeMillis() - (idx * 60000L),
                        description = playlist.description,
                        videoUrl = "https://www.youtube.com/watch?v=$id",
                        isRemote = true
                    )
                    customVideosList.add(video)
                }
            }

            if (customVideosList.isNotEmpty()) {
                playlistCache[playlist.id] = customVideosList
                return@withContext customVideosList
            }
        }

        val rawVideos = feedService.fetchPlaylistVideos(playlist.id, playlist.channelTitle)
        val videos = rawVideos.filter {
            !PredefinedPlaylists.isBlockedYouTubeChannel(it.channelTitle, it.channelId)
        }
        if (videos.isNotEmpty()) {
            playlistCache[playlist.id] = videos
            val firstThumb = videos.firstOrNull()?.thumbnailUrl
            _playlists.value = _playlists.value.map { pl ->
                if (pl.id == playlist.id) {
                    val updatedThumb = if (pl.thumbnailUrl.isNullOrBlank() || pl.thumbnailUrl.contains("/default/")) {
                        firstThumb ?: pl.thumbnailUrl
                    } else {
                        pl.thumbnailUrl
                    }
                    pl.copy(
                        thumbnailUrl = updatedThumb,
                        videoCountEstimate = if ((pl.videoCountEstimate ?: 0) < videos.size) videos.size else pl.videoCountEstimate
                    )
                } else {
                    pl
                }
            }
        }
        videos
    }

    suspend fun fetchMoreChannelVideos(
        channelId: String,
        channelTitle: String,
        pageToken: String?
    ): YouTubePaginatedResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            if (channelId == DailymotionFeedService.CHANNEL_MAIN_ID || channelId == DailymotionFeedService.CHANNEL_VLOG_ID) {
                val page = pageToken?.toIntOrNull() ?: 1
                val dmVideos = dailymotionService.fetchUserVideos(channelId, channelTitle, page = page)
                if (dmVideos.isNotEmpty()) {
                    dao.insertVideos(dmVideos.map { YouTubeVideoEntity.fromDomain(it) })
                }
                val hasMore = dmVideos.size >= 50
                val nextToken = if (hasMore) (page + 1).toString() else null
                return@withContext YouTubePaginatedResult(dmVideos, nextToken, hasMore)
            }

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
