package com.example.util

import java.util.regex.Pattern

enum class VideoPlatform {
    YOUTUBE,
    DAILYMOTION,
    DIRECT_STREAM,
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

    private val DAILYMOTION_PATTERNS = listOf(
        Pattern.compile("(?:https?://)?(?:www\\.)?dailymotion\\.com/video/([a-zA-Z0-9]+)"),
        Pattern.compile("(?:https?://)?(?:www\\.)?dailymotion\\.com/embed/video/([a-zA-Z0-9]+)"),
        Pattern.compile("(?:https?://)?dai\\.ly/([a-zA-Z0-9]+)")
    )

    fun parse(urlOrId: String): ParsedVideoInfo {
        val trimmed = urlOrId.trim()
        if (trimmed.isEmpty()) {
            return ParsedVideoInfo(VideoPlatform.UNKNOWN, "", "")
        }

        // 1. Check for Dailymotion prefix (e.g. dm_x27lzjr_x8abc or dm_x8abc)
        if (trimmed.startsWith("dm_")) {
            val dmId = trimmed.substringAfterLast("_")
            return ParsedVideoInfo(
                platform = VideoPlatform.DAILYMOTION,
                videoId = dmId,
                originalUrl = "https://www.dailymotion.com/video/$dmId",
                embedUrl = "https://www.dailymotion.com/embed/video/$dmId",
                thumbnailUrl = "https://www.dailymotion.com/thumbnail/video/$dmId"
            )
        }

        // 2. Check Dailymotion URL patterns
        for (pattern in DAILYMOTION_PATTERNS) {
            val matcher = pattern.matcher(trimmed)
            if (matcher.find()) {
                val dmId = matcher.group(1).orEmpty()
                return ParsedVideoInfo(
                    platform = VideoPlatform.DAILYMOTION,
                    videoId = dmId,
                    originalUrl = "https://www.dailymotion.com/video/$dmId",
                    embedUrl = "https://www.dailymotion.com/embed/video/$dmId",
                    thumbnailUrl = "https://www.dailymotion.com/thumbnail/video/$dmId"
                )
            }
        }

        // 3. Check for Direct Video Stream / Firebase Storage URL
        val isDirectStream = trimmed.contains("firebasestorage.googleapis.com", ignoreCase = true) ||
                trimmed.contains(".mp4", ignoreCase = true) ||
                trimmed.contains(".m3u8", ignoreCase = true) ||
                trimmed.contains(".webm", ignoreCase = true) ||
                trimmed.contains(".mov", ignoreCase = true) ||
                (trimmed.contains("firebase", ignoreCase = true) && trimmed.startsWith("http") && !trimmed.contains("youtube") && !trimmed.contains("dailymotion"))

        if (isDirectStream) {
            return ParsedVideoInfo(
                platform = VideoPlatform.DIRECT_STREAM,
                videoId = trimmed,
                originalUrl = trimmed,
                embedUrl = trimmed,
                thumbnailUrl = ""
            )
        }

        // 4. Check YouTube patterns
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

        // 5. Check for standard 11-char YouTube ID
        if (trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return ParsedVideoInfo(
                platform = VideoPlatform.YOUTUBE,
                videoId = trimmed,
                originalUrl = "https://www.youtube.com/watch?v=$trimmed",
                embedUrl = "https://www.youtube-nocookie.com/embed/$trimmed",
                thumbnailUrl = "https://i.ytimg.com/vi/$trimmed/hqdefault.jpg"
            )
        }

        // 6. Generic HTTP(S) stream fallback
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            if (!trimmed.contains("youtube") && !trimmed.contains("youtu.be") && !trimmed.contains("dailymotion")) {
                return ParsedVideoInfo(
                    platform = VideoPlatform.DIRECT_STREAM,
                    videoId = trimmed,
                    originalUrl = trimmed,
                    embedUrl = trimmed,
                    thumbnailUrl = ""
                )
            }
        }

        // 7. Default fallback: Treat as YouTube
        return ParsedVideoInfo(
            platform = VideoPlatform.YOUTUBE,
            videoId = trimmed,
            originalUrl = if (trimmed.startsWith("http")) trimmed else "https://www.youtube.com/watch?v=$trimmed",
            embedUrl = "https://www.youtube-nocookie.com/embed/$trimmed",
            thumbnailUrl = "https://i.ytimg.com/vi/$trimmed/hqdefault.jpg"
        )
    }

    fun isDirectStream(urlOrId: String): Boolean {
        return parse(urlOrId).platform == VideoPlatform.DIRECT_STREAM
    }

    fun isYouTube(urlOrId: String): Boolean {
        return parse(urlOrId).platform == VideoPlatform.YOUTUBE
    }

    fun isDailymotion(urlOrId: String): Boolean {
        return parse(urlOrId).platform == VideoPlatform.DAILYMOTION
    }
}
