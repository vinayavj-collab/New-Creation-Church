package com.example.data.remote

import android.util.Log
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

/**
 * Service for fetching video feeds from Dailymotion's REST API.
 * Supports public channels (e.g. x27lzjr - Vinay Kumar AVJ)
 * and Personal Vlog channel (x4sr8o4) under dual-layer security rules.
 */
class DailymotionFeedService {
    private val TAG = "DailymotionFeedService"

    companion object {
        const val CHANNEL_MAIN_ID = "x27lzjr"
        const val CHANNEL_MAIN_TITLE = "Vinay Kumar AVJ (Dailymotion)"
        const val CHANNEL_VLOG_ID = "x4sr8o4"
        const val CHANNEL_VLOG_TITLE = "Vinay AVJ Vlog (Dailymotion)"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun fetchUserVideos(
        userId: String,
        channelTitle: String,
        page: Int = 1,
        limit: Int = 50
    ): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        val url = "https://api.dailymotion.com/user/$userId/videos?fields=id,title,description,thumbnail_720_url,thumbnail_large_url,thumbnail_url,created_time,duration,url&limit=$limit&page=$page"
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Dailymotion API returned code: ${response.code} for user $userId")
                return@withContext emptyList()
            }

            val bodyString = response.body?.string().orEmpty()
            if (bodyString.isBlank()) return@withContext emptyList()

            val jsonObject = JSONObject(bodyString)
            val listArray = jsonObject.optJSONArray("list") ?: return@withContext emptyList()

            val resultList = mutableListOf<YouTubeVideo>()
            for (i in 0 until listArray.length()) {
                val item = listArray.getJSONObject(i)
                val dmId = item.optString("id").trim()
                if (dmId.isBlank()) continue

                val title = item.optString("title", "Dailymotion Video")
                val description = item.optString("description", "")

                val lowerTitle = title.lowercase(Locale.ROOT)
                val lowerDesc = description.lowercase(Locale.ROOT)
                if (lowerTitle.contains("metdaan") || lowerDesc.contains("metdaan") ||
                    lowerTitle.contains("met daan") || lowerDesc.contains("met daan")) {
                    continue
                }
                val thumb720 = item.optString("thumbnail_720_url", "")
                val thumbLarge = item.optString("thumbnail_large_url", "")
                val thumbDefault = item.optString("thumbnail_url", "")
                val bestThumb = when {
                    thumb720.isNotBlank() -> thumb720
                    thumbLarge.isNotBlank() -> thumbLarge
                    thumbDefault.isNotBlank() -> thumbDefault
                    else -> "https://www.dailymotion.com/thumbnail/video/$dmId"
                }

                val createdSec = item.optLong("created_time", 0L)
                val timestamp = if (createdSec > 0) createdSec * 1000L else System.currentTimeMillis()
                val dateStr = dateFormat.format(Date(timestamp))

                val videoItem = YouTubeVideo(
                    id = "dm_${userId}_$dmId",
                    title = title,
                    channelId = userId,
                    channelTitle = channelTitle,
                    thumbnailUrl = bestThumb,
                    publishedAt = dateStr,
                    publishedTimestamp = timestamp,
                    description = description,
                    videoUrl = "https://www.dailymotion.com/video/$dmId"
                )
                resultList.add(videoItem)
            }
            resultList
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Dailymotion videos for $userId", e)
            emptyList()
        }
    }
}
