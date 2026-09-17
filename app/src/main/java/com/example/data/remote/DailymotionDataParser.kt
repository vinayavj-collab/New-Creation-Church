package com.example.data.remote

import com.example.data.model.YouTubeVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class DailymotionVideoMetadata(
    val videoId: String,
    val canonicalUrl: String,
    val embedUrl: String,
    val thumbnailUrl: String,
    val title: String = "",
    val description: String = "",
    val channelId: String = "",
    val channelTitle: String = "",
    val durationSeconds: Int = 0,
    val publishedTimestamp: Long = 0L
)

/**
 * Dedicated Dailymotion data parser.
 * Fetches video links and metadata from the specified partner channels:
 * - Christian Channel: `x27lzjr` (https://www.dailymotion.com/partner/x27lzjr/media/video)
 * - Personal Vlog Channel: `x4sr8o4` (https://www.dailymotion.com/partner/x4sr8o4/media/video)
 *
 * Integrates seamlessly with UniversalVideoPlayer by parsing any Dailymotion URL or ID into
 * normalized video player targets.
 */
class DailymotionDataParser(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        const val PARTNER_CHRISTIAN = "x27lzjr"
        const val PARTNER_VLOG = "x4sr8o4"

        const val PARTNER_CHRISTIAN_URL = "https://www.dailymotion.com/partner/x27lzjr/media/video"
        const val PARTNER_VLOG_URL = "https://www.dailymotion.com/partner/x4sr8o4/media/video"

        private val DM_URL_PATTERNS = listOf(
            Pattern.compile("(?:https?://)?(?:www\\.)?dai\\.ly/([a-zA-Z0-9]+)"),
            Pattern.compile("(?:https?://)?(?:www\\.)?dailymotion\\.com/video/([a-zA-Z0-9]+)"),
            Pattern.compile("(?:https?://)?(?:www\\.)?dailymotion\\.com/embed/video/([a-zA-Z0-9]+)"),
            Pattern.compile("(?:https?://)?(?:www\\.)?dailymotion\\.com/partner/[^/]+/media/video/([a-zA-Z0-9]+)")
        )

        /**
         * Parses any Dailymotion URL, URI, or ID into clean video metadata.
         */
        fun extractVideoMetadata(urlOrId: String): DailymotionVideoMetadata? {
            val trimmed = urlOrId.trim()
            if (trimmed.isEmpty()) return null

            var cleanId: String? = null

            // 1. Regex patterns for standard Dailymotion URLs
            for (pattern in DM_URL_PATTERNS) {
                val matcher = pattern.matcher(trimmed)
                if (matcher.find()) {
                    cleanId = matcher.group(1)
                    break
                }
            }

            // 2. Prefixes like "dm_" or "dm:" or "dailymotion:"
            if (cleanId == null) {
                when {
                    trimmed.startsWith("dm_", ignoreCase = true) -> {
                        cleanId = trimmed.substringAfter("dm_")
                    }
                    trimmed.startsWith("dm:", ignoreCase = true) -> {
                        cleanId = trimmed.substringAfter("dm:")
                    }
                    trimmed.startsWith("dailymotion:", ignoreCase = true) -> {
                        cleanId = trimmed.substringAfter("dailymotion:")
                    }
                    // 3. Raw Dailymotion video ID (typically 'x' followed by 5 to 7 alphanumeric characters)
                    trimmed.matches(Regex("^x[a-zA-Z0-9]{5,8}$")) -> {
                        cleanId = trimmed
                    }
                }
            }

            val id = cleanId ?: return null
            if (id.isBlank()) return null

            return DailymotionVideoMetadata(
                videoId = id,
                canonicalUrl = "https://www.dailymotion.com/video/$id",
                embedUrl = "https://www.dailymotion.com/embed/video/$id?autoplay=1&mute=0",
                thumbnailUrl = "https://www.dailymotion.com/thumbnail/video/$id"
            )
        }

        fun isDailymotionSource(urlOrId: String): Boolean {
            return extractVideoMetadata(urlOrId) != null ||
                    urlOrId.contains("dailymotion.com", ignoreCase = true) ||
                    urlOrId.contains("dai.ly", ignoreCase = true) ||
                    urlOrId.startsWith("dm_", ignoreCase = true)
        }
    }

    /**
     * Fetches video links and metadata directly from the specified partner channel.
     * Supports user uploads, playlists, and channel media.
     */
    suspend fun fetchPartnerVideos(
        partnerChannelId: String,
        page: Int = 1,
        limit: Int = 15
    ): DailymotionPaginatedResult = withContext(Dispatchers.IO) {
        val cleanPartnerId = partnerChannelId.removePrefix("dm_").trim()
        val isVlog = cleanPartnerId == PARTNER_VLOG
        val defaultOwner = if (isVlog) "Pastor Vinay (Personal Vlog)" else "Vinay Kumar AVJ Christian"
        val channelKey = if (isVlog) "dm_$PARTNER_VLOG" else "dm_$PARTNER_CHRISTIAN"

        // Step 1: Query public partner user uploads
        val directUrl = "https://api.dailymotion.com/user/$cleanPartnerId/videos?page=$page&limit=$limit&fields=id,title,description,thumbnail_720_url,thumbnail_480_url,thumbnail_360_url,created_time,owner.screenname,url,duration"
        val primaryResult = executeAndParseDailymotion(directUrl, page, limit, channelKey, defaultOwner)

        if (primaryResult.videos.isNotEmpty()) {
            return@withContext primaryResult
        }

        // Step 2: If user video list is empty (common for channels with organized playlists like x27lzjr),
        // query user's playlists and fetch videos from them
        val playlistVideos = fetchVideosFromUserPlaylists(cleanPartnerId, channelKey, defaultOwner)
        if (playlistVideos.isNotEmpty()) {
            return@withContext DailymotionPaginatedResult(
                videos = playlistVideos,
                page = page,
                limit = limit,
                hasMore = false,
                total = playlistVideos.size
            )
        }

        // Step 3: Fallback query user's favorites
        val favoritesUrl = "https://api.dailymotion.com/user/$cleanPartnerId/favorites?page=$page&limit=$limit&fields=id,title,description,thumbnail_720_url,thumbnail_480_url,thumbnail_360_url,created_time,owner.screenname,url,duration"
        val favResult = executeAndParseDailymotion(favoritesUrl, page, limit, channelKey, defaultOwner)
        if (favResult.videos.isNotEmpty()) {
            return@withContext favResult
        }

        primaryResult
    }

    private suspend fun fetchVideosFromUserPlaylists(
        userId: String,
        channelKey: String,
        defaultOwner: String
    ): List<YouTubeVideo> {
        val playlistsUrl = "https://api.dailymotion.com/user/$userId/playlists?limit=10"
        val jsonStr = httpGet(playlistsUrl) ?: return emptyList()
        val videos = mutableListOf<YouTubeVideo>()

        try {
            val root = JSONObject(jsonStr)
            val list = root.optJSONArray("list") ?: return emptyList()
            for (i in 0 until list.length()) {
                val plObj = list.optJSONObject(i) ?: continue
                val plId = plObj.optString("id", "")
                if (plId.isBlank()) continue

                val plVideosUrl = "https://api.dailymotion.com/playlist/$plId/videos?limit=20&fields=id,title,description,thumbnail_720_url,thumbnail_480_url,thumbnail_360_url,created_time,owner.screenname,url,duration"
                val plVideosResult = executeAndParseDailymotion(plVideosUrl, 1, 20, channelKey, defaultOwner)
                videos.addAll(plVideosResult.videos)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return videos.distinctBy { it.id }
    }

    private fun executeAndParseDailymotion(
        endpoint: String,
        requestedPage: Int,
        requestedLimit: Int,
        channelKey: String,
        defaultOwner: String
    ): DailymotionPaginatedResult {
        val jsonStr = httpGet(endpoint) ?: return DailymotionPaginatedResult(emptyList(), requestedPage, requestedLimit, false)
        return parseDailymotionApiResponse(jsonStr, requestedPage, requestedLimit, channelKey, defaultOwner)
    }

    private fun httpGet(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseDailymotionApiResponse(
        jsonString: String,
        requestedPage: Int,
        requestedLimit: Int,
        channelKey: String,
        defaultOwner: String
    ): DailymotionPaginatedResult {
        val videos = mutableListOf<YouTubeVideo>()
        var hasMore = false
        var total = 0

        try {
            val root = JSONObject(jsonString)
            hasMore = root.optBoolean("has_more", false)
            val page = root.optInt("page", requestedPage)
            val limit = root.optInt("limit", requestedLimit)
            total = root.optInt("total", 0)

            val list = root.optJSONArray("list")
            if (list != null) {
                for (i in 0 until list.length()) {
                    val item = list.optJSONObject(i) ?: continue
                    val rawId = item.optString("id", "")
                    if (rawId.isBlank()) continue

                    val title = item.optString("title", "Dailymotion Video")
                    val description = item.optString("description", "")

                    val thumb720 = item.optString("thumbnail_720_url", "")
                    val thumb480 = item.optString("thumbnail_480_url", "")
                    val thumb360 = item.optString("thumbnail_360_url", "")
                    val thumb = when {
                        thumb720.isNotBlank() -> thumb720
                        thumb480.isNotBlank() -> thumb480
                        thumb360.isNotBlank() -> thumb360
                        else -> "https://www.dailymotion.com/thumbnail/video/$rawId"
                    }

                    val createdSec = item.optLong("created_time", 0L)
                    val createdTimestamp = if (createdSec > 0) createdSec * 1000L else System.currentTimeMillis()
                    val owner = item.optString("owner.screenname", defaultOwner)
                    val videoUrl = item.optString("url", "https://www.dailymotion.com/video/$rawId")

                    val displayDate = if (createdTimestamp > 0) {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(createdTimestamp))
                    } else "Recent"

                    videos.add(
                        YouTubeVideo(
                            id = "dm_$rawId",
                            title = title,
                            channelId = channelKey,
                            channelTitle = owner.ifBlank { defaultOwner },
                            thumbnailUrl = thumb,
                            publishedAt = displayDate,
                            publishedTimestamp = createdTimestamp,
                            description = description,
                            videoUrl = videoUrl
                        )
                    )
                }
            }

            return DailymotionPaginatedResult(
                videos = videos,
                page = page,
                limit = limit,
                hasMore = hasMore || videos.size >= requestedLimit,
                total = if (total > 0) total else videos.size
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return DailymotionPaginatedResult(videos, requestedPage, requestedLimit, false)
    }
}
