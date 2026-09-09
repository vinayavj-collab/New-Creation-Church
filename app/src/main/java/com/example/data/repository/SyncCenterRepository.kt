package com.example.data.repository

import android.content.Context
import com.example.data.bible.repository.BibleRepository
import com.example.data.bible.repository.LyricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class SyncStatus(
    val lastBloggerSync: Long = 0L,
    val lastYouTubeSync: Long = 0L,
    val lastBibleSync: Long = 0L,
    val lastLyricsSync: Long = 0L,
    val isSyncing: Boolean = false,
    val lastSyncMessage: String = ""
)

class SyncCenterRepository(
    private val context: Context,
    private val bloggerRepository: BloggerRepository,
    private val youTubeRepository: YouTubeRepository,
    private val bibleRepository: BibleRepository,
    private val lyricsRepository: LyricsRepository
) {
    private val prefs = context.getSharedPreferences("vinay_sync_prefs", Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(loadStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private fun loadStatus(): SyncStatus {
        return SyncStatus(
            lastBloggerSync = prefs.getLong("last_blogger_sync", System.currentTimeMillis() - 3600000),
            lastYouTubeSync = prefs.getLong("last_youtube_sync", System.currentTimeMillis() - 3600000),
            lastBibleSync = prefs.getLong("last_bible_sync", System.currentTimeMillis() - 7200000),
            lastLyricsSync = prefs.getLong("last_lyrics_sync", System.currentTimeMillis() - 7200000)
        )
    }

    suspend fun syncAll(): Result<String> = withContext(Dispatchers.IO) {
        _status.value = _status.value.copy(isSyncing = true, lastSyncMessage = "Syncing all feeds...")
        try {
            // 1. Sync Blogger
            bloggerRepository.refreshPosts(showPersonalVlog = false)
            val nowBlogger = System.currentTimeMillis()
            prefs.edit().putLong("last_blogger_sync", nowBlogger).apply()

            // 2. Sync YouTube
            youTubeRepository.refreshChannelVideos()
            val nowYT = System.currentTimeMillis()
            prefs.edit().putLong("last_youtube_sync", nowYT).apply()

            // 3. Init Lyrics if needed
            lyricsRepository.initializePreloadedLyrics()
            val nowLyrics = System.currentTimeMillis()
            prefs.edit().putLong("last_lyrics_sync", nowLyrics).apply()

            val nowBible = System.currentTimeMillis()
            prefs.edit().putLong("last_bible_sync", nowBible).apply()

            _status.value = SyncStatus(
                lastBloggerSync = nowBlogger,
                lastYouTubeSync = nowYT,
                lastBibleSync = nowBible,
                lastLyricsSync = nowLyrics,
                isSyncing = false,
                lastSyncMessage = "All feeds synced successfully!"
            )
            Result.success("Sync complete")
        } catch (e: Exception) {
            _status.value = _status.value.copy(isSyncing = false, lastSyncMessage = "Sync failed: ${e.message}")
            Result.failure(e)
        }
    }
}
