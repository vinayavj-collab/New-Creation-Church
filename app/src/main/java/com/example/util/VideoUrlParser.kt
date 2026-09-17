package com.example.util

import com.example.data.remote.DailymotionDataParser
import java.util.regex.Pattern

enum class VideoPlatform {
    YOUTUBE,
    DAILYMOTION,
    UNKNOWN
}

data class ParsedVideoInfo(
    val platform: VideoPlatform,
    val videoId: String,
    val originalUrl: String,
    val embedUrl: String = "",
    val thumbnailUrl: String = ""
)

object VideoUrlParser {

    private val YOUTUBE_PATTERNS = listOf(
        Pattern.compile("(?:https?://)?(?:www\\.|m\\.)?youtu\\.be/([a-zA-Z0-9_-]{11})"),
        Pattern.compile("(?:https?://)?(?:www\\.|m\\.)?youtube\\.com/(?:watch\\?v=|embed/|v/|shorts/|live/)([a-zA-Z0-9_-]{11})"),
        Pattern.compile("(?:https?://)?(?:www\\.|m\\.)?youtube\\.com/.*[?&]v=([a-zA-Z0-9_-]{11})")
    )

    fun parse(urlOrId: String): ParsedVideoInfo {
        val trimmed = urlOrId.trim()
        if (trimmed.isEmpty()) {
            return ParsedVideoInfo(VideoPlatform.UNKNOWN, "", "")
        }

        // 1. Check Dailymotion using dedicated DailymotionDataParser
        val dmMeta = DailymotionDataParser.extractVideoMetadata(trimmed)
        if (dmMeta != null) {
            return ParsedVideoInfo(
                platform = VideoPlatform.DAILYMOTION,
                videoId = dmMeta.videoId,
                originalUrl = dmMeta.canonicalUrl,
                embedUrl = dmMeta.embedUrl,
                thumbnailUrl = dmMeta.thumbnailUrl
            )
        }

        // 2. Check YouTube patterns
        for (pattern in YOUTUBE_PATTERNS) {
            val matcher = pattern.matcher(trimmed)
            if (matcher.find()) {
                val id = matcher.group(1).orEmpty()
                return ParsedVideoInfo(
                    platform = VideoPlatform.YOUTUBE,
                    videoId = id,
                    originalUrl = "https://www.youtube.com/watch?v=$id",
                    embedUrl = "https://www.youtube-nocookie.com/embed/$id",
                    thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
                )
            }
        }

        // 3. Check for standard 11-char YouTube ID
        if (trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return ParsedVideoInfo(
                platform = VideoPlatform.YOUTUBE,
                videoId = trimmed,
                originalUrl = "https://www.youtube.com/watch?v=$trimmed",
                embedUrl = "https://www.youtube-nocookie.com/embed/$trimmed",
                thumbnailUrl = "https://i.ytimg.com/vi/$trimmed/hqdefault.jpg"
            )
        }

        // 4. Check for Dailymotion fallback
        if (DailymotionDataParser.isDailymotionSource(trimmed)) {
            val cleanId = trimmed.removePrefix("dm_").removePrefix("dm:").removePrefix("dailymotion:")
            return ParsedVideoInfo(
                platform = VideoPlatform.DAILYMOTION,
                videoId = cleanId,
                originalUrl = "https://www.dailymotion.com/video/$cleanId",
                embedUrl = "https://www.dailymotion.com/embed/video/$cleanId",
                thumbnailUrl = "https://www.dailymotion.com/thumbnail/video/$cleanId"
            )
        }

        // 5. Default fallback: Treat as YouTube
        return ParsedVideoInfo(
            platform = VideoPlatform.YOUTUBE,
            videoId = trimmed,
            originalUrl = if (trimmed.startsWith("http")) trimmed else "https://www.youtube.com/watch?v=$trimmed",
            embedUrl = "https://www.youtube-nocookie.com/embed/$trimmed",
            thumbnailUrl = "https://i.ytimg.com/vi/$trimmed/hqdefault.jpg"
        )
    }

    fun isDailymotion(urlOrId: String): Boolean {
        return parse(urlOrId).platform == VideoPlatform.DAILYMOTION
    }

    fun isYouTube(urlOrId: String): Boolean {
        return parse(urlOrId).platform == VideoPlatform.YOUTUBE
    }
}
