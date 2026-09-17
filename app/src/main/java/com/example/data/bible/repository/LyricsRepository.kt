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
    private val savedItemRepository: com.example.data.repository.SavedItemRepository? = null,
    private val feedService: BloggerFeedService = BloggerFeedService()
) {
    companion object {
        const val GOOGLE_SHEET_CSV_URL =
            "https://docs.google.com/spreadsheets/d/1GTftiR70HU4KAGEKndUR88blCRvFBbLKcbGQ8IFPAk4/export?format=csv"
    }

    fun getActiveSpreadsheetUrl(): String {
        return try {
            com.example.data.repository.FirebaseDataRepository.getInstance().getSongSpreadsheetUrl()
        } catch (e: Exception) {
            GOOGLE_SHEET_CSV_URL
        }
    }

    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
        .build()

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
        val id = bibleDao.insertSong(song.copy(songNumber = number))
        if (song.isFavorite) {
            syncSongToSavedItems(song.copy(id = id, songNumber = number), isSaved = true)
        }
        id
    }

    suspend fun updateSong(song: ChristianSongEntity) = withContext(Dispatchers.IO) {
        bibleDao.updateSong(song)
        if (song.isFavorite) {
            syncSongToSavedItems(song, isSaved = true)
        }
    }

    suspend fun toggleFavorite(song: ChristianSongEntity): Boolean = withContext(Dispatchers.IO) {
        val newFavState = !song.isFavorite
        val updatedSong = song.copy(isFavorite = newFavState, modifiedAt = System.currentTimeMillis())
        bibleDao.updateSong(updatedSong)
        syncSongToSavedItems(updatedSong, isSaved = newFavState)
        newFavState
    }

    suspend fun setFavorite(songId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        bibleDao.setSongFavorite(songId, isFavorite)
        val song = bibleDao.getSongById(songId)
        if (song != null) {
            syncSongToSavedItems(song.copy(isFavorite = isFavorite), isSaved = isFavorite)
        } else if (!isFavorite) {
            savedItemRepository?.remove("SONG_$songId")
        }
    }

    private suspend fun syncSongToSavedItems(song: ChristianSongEntity, isSaved: Boolean) {
        if (savedItemRepository == null) return
        val savedId = "SONG_${song.id}"
        if (isSaved) {
            val subtitleSnippet = song.content
                .lines()
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString(" • ")
                .take(100)

            savedItemRepository.save(
                com.example.data.local.SavedItemEntity(
                    id = savedId,
                    type = "SONG",
                    title = if (song.songNumber > 0) "#${song.songNumber} - ${song.title}" else song.title,
                    subtitle = if (subtitleSnippet.isNotBlank()) subtitleSnippet else "मसीही गीत (Christian Song)",
                    imageUrl = null,
                    url = null,
                    extraDataJson = song.id.toString(),
                    savedTimestamp = System.currentTimeMillis()
                )
            )
        } else {
            savedItemRepository.remove(savedId)
        }
    }

    suspend fun deleteSong(id: Long) = withContext(Dispatchers.IO) {
        bibleDao.deleteSongById(id)
        savedItemRepository?.remove("SONG_$id")
    }

    /**
     * Fetches song data directly from the Google Sheet CSV endpoint:
     * https://docs.google.com/spreadsheets/d/1GTftiR70HU4KAGEKndUR88blCRvFBbLKcbGQ8IFPAk4/export?format=csv
     * Maps Column A -> song_number, Column B -> song_title, Column C -> lyrics.
     * Stores in local SQLite Room database for complete offline access.
     */
    suspend fun syncLyricsFromGoogleSheet(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = getActiveSpreadsheetUrl()
            val request = okhttp3.Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Android) VinayKumarAVJ/SongBook")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error code: ${response.code}"))
                }
                val csvContent = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                val records = parseCsv(csvContent)
                if (records.isEmpty()) {
                    return@withContext Result.success(0)
                }

                var syncCount = 0
                for ((index, row) in records.withIndex()) {
                    // Skip header row if it contains header keywords or is the first row with non-numeric first col
                    val col0 = row.getOrNull(0)?.trim().orEmpty()
                    val col1 = row.getOrNull(1)?.trim().orEmpty()
                    val col2 = row.getOrNull(2).orEmpty()

                    if (index == 0 && (col0.contains("संख्या") || col0.contains("song", ignoreCase = true) || col1.contains("शीर्षक") || col1.contains("title", ignoreCase = true))) {
                        continue
                    }

                    // Extract song number from column A
                    val songNumber = "\\d+".toRegex().find(col0)?.value?.toIntOrNull()
                        ?: col0.toIntOrNull()
                        ?: 0

                    // Clean lyrics from column C, rendering \n line breaks properly
                    val cleanLyrics = col2
                        .replace("\\r\\n", "\n")
                        .replace("\\n", "\n")
                        .replace("\r\n", "\n")
                        .replace("\r", "\n")
                        .trim()

                    // Skip completely empty rows
                    if (col1.isBlank() && cleanLyrics.isBlank()) {
                        continue
                    }

                    val title = if (col1.isNotBlank()) {
                        col1
                    } else {
                        cleanLyrics.lines().firstOrNull()?.take(50).orEmpty().ifBlank { "गीत #$songNumber" }
                    }

                    // Check if already in database by songNumber or title
                    val existing = if (songNumber > 0) {
                        bibleDao.getSongByNumber(songNumber)
                    } else {
                        bibleDao.getSongByTitle(title)
                    }

                    if (existing != null) {
                        // Update existing song details while preserving favorites and custom user notes
                        val updated = existing.copy(
                            songNumber = if (songNumber > 0) songNumber else existing.songNumber,
                            title = title,
                            content = if (cleanLyrics.isNotBlank()) cleanLyrics else existing.content,
                            personalNotes = if (existing.personalNotes.isBlank()) "Synced from Google Sheets" else existing.personalNotes,
                            modifiedAt = System.currentTimeMillis()
                        )
                        bibleDao.updateSong(updated)
                        if (updated.isFavorite) {
                            syncSongToSavedItems(updated, isSaved = true)
                        }
                    } else {
                        // Insert brand new song from Google Sheets
                        val newSongNumber = if (songNumber > 0) songNumber else ((bibleDao.getMaxSongNumber() ?: 0) + 1)
                        val newSong = ChristianSongEntity(
                            songNumber = newSongNumber,
                            title = title,
                            content = cleanLyrics,
                            artist = "Christian Worship",
                            category = "स्तुति व आराधना",
                            keyScale = "D",
                            colorHex = "#FFFBEB",
                            personalNotes = "Synced from Google Sheets",
                            isFavorite = false,
                            isUserCreated = false,
                            createdAt = System.currentTimeMillis(),
                            modifiedAt = System.currentTimeMillis()
                        )
                        bibleDao.insertSong(newSong)
                    }
                    syncCount++
                }

                Result.success(syncCount)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * RFC 4180 compliant CSV parser that supports multiline fields in quotes,
     * escaped double quotes, and CRLF / LF line endings.
     */
    private fun parseCsv(csvText: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = csvText.length

        while (i < len) {
            val c = csvText[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && csvText[i + 1] == '"') {
                        currentField.append('"')
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRecord.add(currentField.toString())
                        currentField.clear()
                    }
                    '\r' -> {
                        if (i + 1 < len && csvText[i + 1] == '\n') {
                            i++
                        }
                        currentRecord.add(currentField.toString())
                        currentField.clear()
                        records.add(currentRecord.toList())
                        currentRecord.clear()
                    }
                    '\n' -> {
                        currentRecord.add(currentField.toString())
                        currentField.clear()
                        records.add(currentRecord.toList())
                        currentRecord.clear()
                    }
                    else -> currentField.append(c)
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString())
            records.add(currentRecord.toList())
        }
        return records
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
            // Seed Song #1 for instant offline accessibility out of the box
            val defaultSong = ChristianSongEntity(
                songNumber = 1,
                title = "आज का दिन",
                content = "आज का दिन यहोवा ने बनाया है,\nहम उसमें आनंदित हो आनंदित हों\n\nआज का दिन यहोवा ने बनाया है,\nहम उसमें आनंदित हो आनंदित हों\n\nप्रभु को महिमा मिले, चाहे हो मेरा अपमान\nवो बढ़े मैं घटूँ, रहे उसी का ध्यान\n\nप्रभु को महिमा मिले, चाहे हो मेरा अपमान\nवो बढ़े मैं घटूँ, रहे उसी का ध्यान\n\nआज का दिन यहोवा ने बनाया है,\nहम उसमें आनंदित हो आनंदित हों\n\nस्तुति प्रशंसा करें, क्यों ना कुछ होता रहे\nउसको हम भाते रहें, चाहे जहाँ भी रहें\n\nस्तुति प्रशंसा करें, क्यों ना कुछ होता रहे\nउसको हम भाते रहें, चाहे जहाँ भी रहें\n\nआज का दिन यहोवा ने बनाया है,\nहम उसमें आनंदित हो आनंदित हों",
                artist = "Christian Worship",
                category = "स्तुति व आराधना",
                keyScale = "D",
                colorHex = "#FFFBEB",
                personalNotes = "Offline Christian Song Book",
                isFavorite = false,
                isUserCreated = false,
                createdAt = System.currentTimeMillis()
            )
            bibleDao.insertSong(defaultSong)
        }
        // Background silent auto-sync from Google Sheets CSV
        try {
            syncLyricsFromGoogleSheet()
        } catch (e: Exception) {
            e.printStackTrace()
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
