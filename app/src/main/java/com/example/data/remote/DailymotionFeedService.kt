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

data class DailymotionPaginatedResult(
    val videos: List<YouTubeVideo>,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean,
    val total: Int = 0
)

/**
 * Service for fetching Dailymotion videos using Dailymotion Public REST API with pagination parameters:
 * Supports `page` (1-based index), `limit`, user/channel search, and tags.
 * Includes dedicated channels:
 * 1. Christian Channel: `x27lzjr` (https://www.dailymotion.com/partner/x27lzjr/media/video)
 * 2. Personal Vlog Channel: `x4sr8o4` (https://www.dailymotion.com/partner/x4sr8o4/media/video)
 */
class DailymotionFeedService(
    private val dataParser: DailymotionDataParser = DailymotionDataParser()
) {
    companion object {
        const val CHRISTIAN_USER_ID = DailymotionDataParser.PARTNER_CHRISTIAN
        const val PERSONAL_VLOG_USER_ID = DailymotionDataParser.PARTNER_VLOG
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchVideos(
        page: Int = 1,
        limit: Int = 15,
        searchQuery: String = "Vinay Kumar AVJ christian hindi worship",
        channelOrUser: String? = null
    ): DailymotionPaginatedResult = withContext(Dispatchers.IO) {
        if (!channelOrUser.isNullOrBlank()) {
            return@withContext dataParser.fetchPartnerVideos(channelOrUser, page, limit)
        }
        val encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8")
        val endpoint = "https://api.dailymotion.com/videos?search=$encodedQuery&page=$page&limit=$limit&fields=id,title,description,thumbnail_720_url,thumbnail_480_url,thumbnail_360_url,created_time,owner.screenname,url,duration"

        val request = Request.Builder()
            .url(endpoint)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DailymotionPaginatedResult(emptyList(), page, limit, hasMore = false)
                }
                val bodyStr = response.body?.string() ?: return@withContext DailymotionPaginatedResult(emptyList(), page, limit, hasMore = false)
                dataParser.parseDailymotionApiResponse(bodyStr, page, limit, "dm_search", "Vinay Kumar AVJ Christian")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DailymotionPaginatedResult(emptyList(), page, limit, hasMore = false)
        }
    }

    suspend fun fetchChristianVideos(page: Int = 1, limit: Int = 15): DailymotionPaginatedResult {
        return dataParser.fetchPartnerVideos(CHRISTIAN_USER_ID, page, limit)
    }

    suspend fun fetchPersonalVlogVideos(page: Int = 1, limit: Int = 15): DailymotionPaginatedResult {
        return dataParser.fetchPartnerVideos(PERSONAL_VLOG_USER_ID, page, limit)
    }

    private fun parseDailymotionJson(jsonString: String, requestedPage: Int, requestedLimit: Int, channelUser: String? = null): DailymotionPaginatedResult {
        val videos = mutableListOf<YouTubeVideo>()
        var hasMore = false
        var total = 0
        try {
            val root = JSONObject(jsonString)
            hasMore = root.optBoolean("has_more", false)
            val page = root.optInt("page", requestedPage)
            val limit = root.optInt("limit", requestedLimit)
            total = root.optInt("total", 0)

            val isVlog = channelUser == PERSONAL_VLOG_USER_ID
            val defaultOwner = if (isVlog) "Pastor Vinay (Personal Vlog)" else "Vinay Kumar AVJ Christian"
            val defaultChannelId = if (isVlog) "dm_$PERSONAL_VLOG_USER_ID" else "dm_$CHRISTIAN_USER_ID"

            val list = root.optJSONArray("list")
            if (list != null) {
                for (i in 0 until list.length()) {
                    val item = list.optJSONObject(i) ?: continue
                    val id = item.optString("id", "")
                    val title = item.optString("title", "Dailymotion Video")
                    val description = item.optString("description", "")
                    val videoUrl = item.optString("url", "https://www.dailymotion.com/video/$id")

                    // Hide specified excluded videos
                    if (id.equals("x2dzbsk", ignoreCase = true) ||
                        videoUrl.contains("x2dzbsk", ignoreCase = true) ||
                        title.contains("JOEL OSTEEN", ignoreCase = true)
                    ) {
                        continue
                    }
                    var thumb = item.optString("thumbnail_480_url", "")
                    if (thumb.isBlank()) {
                        thumb = item.optString("thumbnail_360_url", "https://www.dailymotion.com/thumbnail/video/$id")
                    }
                    val createdTime = item.optLong("created_time", 0L) * 1000L
                    val owner = item.optString("owner.screenname", defaultOwner)

                    val displayDate = if (createdTime > 0) {
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        sdf.format(Date(createdTime))
                    } else "Recent"

                    if (id.isNotBlank()) {
                        videos.add(
                            YouTubeVideo(
                                id = "dm_$id",
                                title = title,
                                channelId = defaultChannelId,
                                channelTitle = owner.ifBlank { defaultOwner },
                                thumbnailUrl = thumb,
                                publishedAt = displayDate,
                                publishedTimestamp = if (createdTime > 0) createdTime else System.currentTimeMillis(),
                                description = description,
                                videoUrl = videoUrl
                            )
                        )
                    }
                }
            }
            return DailymotionPaginatedResult(
                videos = videos,
                page = page,
                limit = limit,
                hasMore = hasMore || videos.size >= requestedLimit,
                total = total
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return DailymotionPaginatedResult(videos, requestedPage, requestedLimit, hasMore = false)
    }
}
