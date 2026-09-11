package com.example.data.bible.repository

import com.example.data.bible.local.BibleDao
import com.example.data.bible.local.ChristianSongEntity
import com.example.data.model.BlogPost
import com.example.data.model.BlogSourceType
import com.example.data.remote.BloggerFeedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class LyricsRepository(
    private val bibleDao: BibleDao,
    private val feedService: BloggerFeedService = BloggerFeedService()
) {

    fun getAllSongs(): Flow<List<ChristianSongEntity>> = bibleDao.getAllSongs()

    fun getFavoriteSongs(): Flow<List<ChristianSongEntity>> = bibleDao.getFavoriteSongs()

    fun searchSongs(query: String): Flow<List<ChristianSongEntity>> = bibleDao.searchSongs(query)

    suspend fun getSongById(id: Long): ChristianSongEntity? = withContext(Dispatchers.IO) {
        bibleDao.getSongById(id)
    }

    suspend fun getSongByNumber(number: Int): ChristianSongEntity? = withContext(Dispatchers.IO) {
        bibleDao.getSongByNumber(number)
    }

    suspend fun getNextSongNumber(): Int = withContext(Dispatchers.IO) {
        (bibleDao.getMaxSongNumber() ?: 0) + 1
    }

    suspend fun addSong(song: ChristianSongEntity): Long = withContext(Dispatchers.IO) {
        val number = if (song.songNumber > 0) song.songNumber else ((bibleDao.getMaxSongNumber() ?: 0) + 1)
        bibleDao.insertSong(song.copy(songNumber = number))
    }

    suspend fun updateSong(song: ChristianSongEntity) = withContext(Dispatchers.IO) {
        bibleDao.updateSong(song)
    }

    suspend fun toggleFavorite(song: ChristianSongEntity) = withContext(Dispatchers.IO) {
        bibleDao.updateSong(song.copy(isFavorite = !song.isFavorite, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun deleteSong(id: Long) = withContext(Dispatchers.IO) {
        bibleDao.deleteSongById(id)
    }

    /**
     * Automatically fetches blog posts from the official Fellowship/Ministry Blogger feed (NOT personal),
     * filters out posts tagged or titled with lyrics/songs, auto-numbers, and saves them into the Song Book.
     */
    suspend fun fetchAndSyncLyricsFromBlogs(clearExisting: Boolean = false): Int = withContext(Dispatchers.IO) {
        try {
            if (clearExisting) {
                bibleDao.deleteAllNonCustomSongs()
            }
            // Fetch strictly from Fellowship / Ministry Blogger feed (NOT personal vlog)
            val fellowshipPosts = feedService.fetchBlogPosts(BlogSourceType.FELLOWSHIP_EVENTS)
            syncLyricsFromBlogPosts(fellowshipPosts)
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun clearAllAndResync(): Int = withContext(Dispatchers.IO) {
        bibleDao.clearAllSongs()
        fetchAndSyncLyricsFromBlogs(clearExisting = false)
    }

    /**
     * Inspects a list of BlogPosts, identifies any lyrics or song posts by tags / title,
     * cleans and formats their HTML content, extracts chords / scale / scripture references,
     * and assigns sequential song numbers.
     */
    suspend fun syncLyricsFromBlogPosts(posts: List<BlogPost>): Int = withContext(Dispatchers.IO) {
        val existingSongs = bibleDao.getAllSongs().first()
        val existingTitles = existingSongs.map { it.title.trim().lowercase() }.toSet()
        val existingBlogIds = existingSongs.mapNotNull { if (it.blogPostId.isNotBlank()) it.blogPostId else null }.toSet()

        var currentMaxNumber = bibleDao.getMaxSongNumber() ?: 0
        var newSongsAdded = 0

        val lyricsTags = setOf(
            "lyrics", "song", "songs", "geet", "गीत", "भजन", "worship", "chords",
            "stuti", "aradhana", "गाने", "गाना", "मसीही गीत", "christian song",
            "christian songs", "sadri song", "sadri geet", "bhajan", "psalm", "hymn", "hymns",
            "स्तुति", "आराधना", "साद्री गीत", "भजन संहिता"
        )

        for (post in posts) {
            val hasMatchingLabel = post.labels.any { label ->
                val l = label.trim().lowercase()
                lyricsTags.any { tag -> l.contains(tag) }
            }
            val titleLower = post.title.trim().lowercase()
            val hasMatchingTitle = lyricsTags.any { tag -> titleLower.contains(tag) } ||
                    titleLower.startsWith("गीत") ||
                    titleLower.contains("lyrics") ||
                    titleLower.contains("chords")

            if (!hasMatchingLabel && !hasMatchingTitle) {
                continue
            }

            // Check if already in database
            if (existingBlogIds.contains(post.id) || existingTitles.contains(titleLower)) {
                continue
            }

            // Clean lyrics content
            val cleanLyrics = cleanHtmlToLyrics(post.contentHtml, post.plainTextExcerpt)
            if (cleanLyrics.length < 15) {
                continue // Skip empty or invalid content
            }

            // Extract Scale / Key
            val scalePattern = Pattern.compile("(?i)(?:Scale|Key|स्केल|स्वर)\\s*[:=-]\\s*([A-G][#b]?(?:m|maj|min)?)")
            val scaleMatcher = scalePattern.matcher(post.contentHtml + " " + post.title)
            val extractedScale = if (scaleMatcher.find()) scaleMatcher.group(1)?.trim() ?: "D" else "D"

            // Extract Scripture / Bible Reference
            val refPattern = Pattern.compile("(?i)(?:Ref|Reference|वचन|संदर्भ|Bible)\\s*[:=-]\\s*([^\n<,]+)")
            val refMatcher = refPattern.matcher(post.contentHtml + " " + post.title)
            val extractedRef = if (refMatcher.find()) refMatcher.group(1)?.trim() ?: "" else ""

            // Determine Category
            val category = when {
                post.labels.any { it.contains("sadri", ignoreCase = true) || it.contains("साद्री", ignoreCase = true) } -> "साद्री मसीही गीत"
                post.labels.any { it.contains("prayer", ignoreCase = true) || it.contains("प्रार्थना", ignoreCase = true) } -> "प्रार्थना व विनती"
                post.labels.any { it.contains("psalm", ignoreCase = true) || it.contains("भजन", ignoreCase = true) } -> "भजन संहिता"
                post.labels.any { it.contains("english", ignoreCase = true) } -> "अंग्रेज़ी व हिंदी"
                post.labels.any { it.contains("christmas", ignoreCase = true) || it.contains("क्रिसमस", ignoreCase = true) } -> "क्रिसमस / सुसमाचार"
                else -> "स्तुति व आराधना"
            }

            currentMaxNumber += 1
            val songEntity = ChristianSongEntity(
                songNumber = currentMaxNumber,
                title = post.title.trim(),
                content = cleanLyrics,
                artist = "Vinay Kumar AVJ",
                category = category,
                keyScale = extractedScale,
                colorHex = "#FFFBEB",
                linkedReferences = extractedRef,
                personalNotes = "Synced from Blogger: ${post.publishedDate}",
                blogPostId = post.id,
                isFavorite = false,
                isUserCreated = false,
                createdAt = post.publishedTimestamp.takeIf { it > 0 } ?: System.currentTimeMillis()
            )

            bibleDao.insertSong(songEntity)
            newSongsAdded++
        }

        newSongsAdded
    }

    suspend fun initializePreloadedLyrics() = withContext(Dispatchers.IO) {
        val count = bibleDao.getSongCount()
        if (count == 0) {
            fetchAndSyncLyricsFromBlogs(clearExisting = false)
        }
    }

    private fun cleanHtmlToLyrics(html: String, excerpt: String): String {
        if (html.isBlank()) return excerpt.trim()
        val text = html
            .replace("(?i)<br\\s*/?>".toRegex(), "\n")
            .replace("(?i)</p>".toRegex(), "\n\n")
            .replace("(?i)<p[^>]*>".toRegex(), "")
            .replace("(?i)</div>".toRegex(), "\n")
            .replace("(?i)<div[^>]*>".toRegex(), "")
            .replace("(?i)</li>".toRegex(), "\n")
            .replace("(?i)<li[^>]*>".toRegex(), "• ")
            .replace("(?i)</h1>|</h2>|</h3>|</h4>".toRegex(), "\n\n")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("<[^>]*>".toRegex(), "")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\n{3,}".toRegex(), "\n\n")
            .trim()

        return if (text.isNotBlank()) text else excerpt.trim()
    }
}
