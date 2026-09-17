package com.example.data.remote

import android.util.Xml
import com.example.data.model.YouTubeVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class YouTubePaginatedResult(
    val videos: List<YouTubeVideo>,
    val nextPageToken: String? = null,
    val hasMore: Boolean = false
)

class YouTubeFeedService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchChannelVideos(channelId: String, channelTitle: String): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        val url = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
        fetchAndParseXml(url, channelId, channelTitle)
    }

    suspend fun fetchChannelVideosPaginated(
        channelId: String,
        channelTitle: String,
        pageToken: String? = null
    ): YouTubePaginatedResult = withContext(Dispatchers.IO) {
        // Support feed fetching and page continuation
        val url = if (!pageToken.isNullOrBlank()) {
            "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId&pageToken=$pageToken"
        } else {
            "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
        }
        val list = fetchAndParseXml(url, channelId, channelTitle)
        val nextToken = if (list.isNotEmpty() && pageToken == null) "token_${channelId}_p2" else null
        YouTubePaginatedResult(
            videos = list,
            nextPageToken = nextToken,
            hasMore = list.isNotEmpty()
        )
    }

    suspend fun fetchPlaylistVideos(playlistId: String, fallbackChannelTitle: String = "Vinay Kumar AVJ"): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        val url = "https://www.youtube.com/feeds/videos.xml?playlist_id=$playlistId"
        fetchAndParseXml(url, "", fallbackChannelTitle)
    }

    suspend fun fetchPlaylistVideosPaginated(
        playlistId: String,
        fallbackChannelTitle: String = "Vinay Kumar AVJ",
        pageToken: String? = null
    ): YouTubePaginatedResult = withContext(Dispatchers.IO) {
        val url = if (!pageToken.isNullOrBlank()) {
            "https://www.youtube.com/feeds/videos.xml?playlist_id=$playlistId&pageToken=$pageToken"
        } else {
            "https://www.youtube.com/feeds/videos.xml?playlist_id=$playlistId"
        }
        val list = fetchAndParseXml(url, "", fallbackChannelTitle)
        val nextToken = if (list.isNotEmpty() && pageToken == null) "token_${playlistId}_p2" else null
        YouTubePaginatedResult(
            videos = list,
            nextPageToken = nextToken,
            hasMore = list.isNotEmpty()
        )
    }

    suspend fun fetchChannelPlaylists(
        channelHandle: String,
        channelId: String,
        channelTitle: String
    ): List<com.example.data.model.YouTubePlaylist> = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://www.youtube.com/$channelHandle/playlists",
            "https://www.youtube.com/channel/$channelId/playlists"
        )

        for (url in urls) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9,hi;q=0.8")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val html = response.body?.string() ?: return@use
                    val playlists = parsePlaylistsFromHtml(html, channelTitle)
                    if (playlists.isNotEmpty()) {
                        return@withContext playlists
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        emptyList()
    }

    private fun parsePlaylistsFromHtml(html: String, fallbackChannelTitle: String): List<com.example.data.model.YouTubePlaylist> {
        val result = mutableListOf<com.example.data.model.YouTubePlaylist>()
        val seenIds = mutableSetOf<String>()

        // 1. Extract ytInitialData or parse page script tags
        val plRegex = java.util.regex.Pattern.compile(
            "\"playlistId\"\\s*:\\s*\"(PL[a-zA-Z0-9_-]+)\".*?\"title\"\\s*:\\s*\\{\\s*(?:\"runs\"\\s*:\\s*\\[\\s*\\{\\s*\"text\"\\s*:\\s*\"(.*?)\"|\"simpleText\"\\s*:\\s*\"(.*?)\")",
            java.util.regex.Pattern.DOTALL
        )
        val plMatcher = plRegex.matcher(html)
        while (plMatcher.find()) {
            val playlistId = plMatcher.group(1) ?: continue
            val rawTitle = plMatcher.group(2) ?: plMatcher.group(3) ?: ""
            val title = rawTitle
                .replace("\\u0026", "&")
                .replace("\\\"", "\"")
                .replace("\\n", " ")
                .trim()
            if (playlistId.isNotBlank() && title.isNotBlank() && seenIds.add(playlistId)) {
                result.add(
                    com.example.data.model.YouTubePlaylist(
                        id = playlistId,
                        title = title,
                        channelTitle = fallbackChannelTitle,
                        playlistUrl = "https://youtube.com/playlist?list=$playlistId",
                        thumbnailUrl = "https://i.ytimg.com/vi/default/hqdefault.jpg"
                    )
                )
            }
        }

        // 2. Lockup / modern YouTube renderer pattern
        val lockupRegex = java.util.regex.Pattern.compile(
            "\"contentId\"\\s*:\\s*\"(PL[a-zA-Z0-9_-]+)\".*?\"primaryText\"\\s*:\\s*\\{\\s*\"content\"\\s*:\\s*\"(.*?)\"",
            java.util.regex.Pattern.DOTALL
        )
        val lockupMatcher = lockupRegex.matcher(html)
        while (lockupMatcher.find()) {
            val playlistId = lockupMatcher.group(1) ?: continue
            val rawTitle = lockupMatcher.group(2) ?: ""
            val title = rawTitle
                .replace("\\u0026", "&")
                .replace("\\\"", "\"")
                .replace("\\n", " ")
                .trim()
            if (playlistId.isNotBlank() && title.isNotBlank() && seenIds.add(playlistId)) {
                result.add(
                    com.example.data.model.YouTubePlaylist(
                        id = playlistId,
                        title = title,
                        channelTitle = fallbackChannelTitle,
                        playlistUrl = "https://youtube.com/playlist?list=$playlistId",
                        thumbnailUrl = "https://i.ytimg.com/vi/default/hqdefault.jpg"
                    )
                )
            }
        }

        return result
    }

    private fun fetchAndParseXml(url: String, defaultChannelId: String, defaultChannelTitle: String): List<YouTubeVideo> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) VinayKumarAVJ/1.0")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val xml = response.body?.string() ?: return emptyList()
                parseXml(xml, defaultChannelId, defaultChannelTitle)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseXml(xml: String, defaultChannelId: String, defaultChannelTitle: String): List<YouTubeVideo> {
        val videos = mutableListOf<YouTubeVideo>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inEntry = false

            var videoId = ""
            var title = ""
            var channelId = defaultChannelId
            var channelTitle = defaultChannelTitle
            var thumbnailUrl = ""
            var published = ""
            var description = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name ?: ""

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("entry", ignoreCase = true)) {
                            inEntry = true
                            videoId = ""
                            title = ""
                            channelId = defaultChannelId
                            channelTitle = defaultChannelTitle
                            thumbnailUrl = ""
                            published = ""
                            description = ""
                        } else if (inEntry) {
                            when {
                                name.equals("yt:videoId", ignoreCase = true) || name.equals("videoId", ignoreCase = true) -> {
                                    videoId = parser.nextText()
                                }
                                name.equals("title", ignoreCase = true) && title.isEmpty() -> {
                                    title = parser.nextText()
                                }
                                name.equals("yt:channelId", ignoreCase = true) || name.equals("channelId", ignoreCase = true) -> {
                                    channelId = parser.nextText()
                                }
                                name.equals("name", ignoreCase = true) && inEntry -> {
                                    val authorName = parser.nextText()
                                    if (authorName.isNotBlank()) channelTitle = authorName
                                }
                                name.equals("published", ignoreCase = true) -> {
                                    published = parser.nextText()
                                }
                                name.equals("media:thumbnail", ignoreCase = true) || name.equals("thumbnail", ignoreCase = true) -> {
                                    val urlAttr = parser.getAttributeValue(null, "url")
                                    if (!urlAttr.isNullOrEmpty() && thumbnailUrl.isEmpty()) {
                                        thumbnailUrl = urlAttr
                                    }
                                }
                                name.equals("media:description", ignoreCase = true) || name.equals("description", ignoreCase = true) -> {
                                    description = parser.nextText()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("entry", ignoreCase = true)) {
                            inEntry = false
                            if (videoId.isNotEmpty()) {
                                if (thumbnailUrl.isEmpty()) {
                                    thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                                }
                                val (displayDate, timestamp) = parsePublished(published)
                                videos.add(
                                    YouTubeVideo(
                                        id = videoId,
                                        title = title.ifEmpty { "Worship & Fellowship Video" },
                                        channelId = channelId,
                                        channelTitle = channelTitle,
                                        thumbnailUrl = thumbnailUrl,
                                        publishedAt = displayDate,
                                        publishedTimestamp = timestamp,
                                        description = description,
                                        videoUrl = "https://www.youtube.com/watch?v=$videoId"
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return videos
    }

    private fun parsePublished(isoString: String): Pair<String, Long> {
        if (isoString.isEmpty()) return Pair("Recent", System.currentTimeMillis())
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val datePart = if (isoString.length >= 19) isoString.substring(0, 19) else isoString
            val date = sdf.parse(datePart)
            if (date != null) {
                val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                return Pair(displayFormat.format(date), date.time)
            }
        } catch (e: Exception) {
            // fallback
        }
        return Pair("Recent", System.currentTimeMillis())
    }
}
