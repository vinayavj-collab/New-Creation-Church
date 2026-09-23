package com.example.data.model

import androidx.annotation.Keep

data class YouTubeVideo(
    val id: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val publishedAt: String,
    val publishedTimestamp: Long,
    val description: String = "",
    val videoUrl: String = "https://www.youtube.com/watch?v=$id",
    val isRemote: Boolean = false,
    val isPinned: Boolean = false
)

@Keep
data class VideoQuickAccessItem(
    val id: String = "",
    val label: String = "",
    val filterType: String = "ALL", // "ALL", "CHANNEL", "KEYWORD"
    val filterValue: String = "",
    val isVisible: Boolean = true,
    val isPinned: Boolean = false,
    val order: Int = 0
)

@Keep
data class VideoQuickAccessConfig(
    val isBarVisible: Boolean = true,
    val items: List<VideoQuickAccessItem> = defaultVideoQuickAccessItems()
)

fun defaultVideoQuickAccessItems(): List<VideoQuickAccessItem> = listOf(
    VideoQuickAccessItem(id = "all", label = "ALL", filterType = "ALL", filterValue = "", isVisible = true, isPinned = true, order = 0),
    VideoQuickAccessItem(id = "worship", label = "Worship", filterType = "CHANNEL", filterValue = "UC92tSCn2I6lwcUyAdyS_MMw", isVisible = true, isPinned = false, order = 1),
    VideoQuickAccessItem(id = "vinay_kumar", label = "Vinay Kumar AVJ", filterType = "CHANNEL", filterValue = "UClFK75L0wsDMf10Tj77hlsg", isVisible = true, isPinned = false, order = 2),
    VideoQuickAccessItem(id = "dailymotion", label = "Dailymotion", filterType = "CHANNEL", filterValue = "dailymotion", isVisible = true, isPinned = false, order = 3)
)

data class YouTubePlaylist(
    val id: String,
    val title: String,
    val channelTitle: String,
    val playlistUrl: String,
    val videoCountEstimate: Int? = null,
    val thumbnailUrl: String? = null
)

data class YouTubeChannelInfo(
    val id: String,
    val name: String,
    val handle: String,
    val channelUrl: String,
    val description: String,
    val avatarUrl: String
)

object PredefinedPlaylists {
    val items = listOf(
        YouTubePlaylist(
            id = "PLL2gT_B-EKC5HYcBdJS4dISi60gB7fcwX",
            title = "Vinay's Songs",
            channelTitle = "Vinay Kumar AVJ",
            playlistUrl = "https://youtube.com/playlist?list=PLL2gT_B-EKC5HYcBdJS4dISi60gB7fcwX"
        ),
        YouTubePlaylist(
            id = "PLD4J90_dqdao",
            title = "Kids",
            channelTitle = "Vinay Kumar AVJ",
            playlistUrl = "https://youtube.com/playlist?list=PLD4J90_dqdao"
        ),
        YouTubePlaylist(
            id = "PLO38jfLAcnuM",
            title = "हिंदी मसीही गीत",
            channelTitle = "Vinay Kumar AVJ Worship",
            playlistUrl = "https://youtube.com/playlist?list=PLO38jfLAcnuM"
        ),
        YouTubePlaylist(
            id = "PLJk1M-aAQVI8",
            title = "सादरी गीत",
            channelTitle = "Vinay Kumar AVJ Worship",
            playlistUrl = "https://youtube.com/playlist?list=PLJk1M-aAQVI8"
        ),
        YouTubePlaylist(
            id = "PLEYaIGI1bhRM",
            title = "English Songs",
            channelTitle = "Vinay Kumar AVJ Worship",
            playlistUrl = "https://youtube.com/playlist?list=PLEYaIGI1bhRM"
        ),
        YouTubePlaylist(
            id = "PLZ5FG_M-N7VE",
            title = "Other Language Songs",
            channelTitle = "Vinay Kumar AVJ Worship",
            playlistUrl = "https://youtube.com/playlist?list=PLZ5FG_M-N7VE"
        ),
        YouTubePlaylist(
            id = "PLHxgaK-u6bsc",
            title = "Sermons",
            channelTitle = "Vinay Kumar AVJ",
            playlistUrl = "https://youtube.com/playlist?list=PLHxgaK-u6bsc"
        ),
        YouTubePlaylist(
            id = "PLKFeMJzFpj5E",
            title = "प्रचार",
            channelTitle = "Vinay Kumar AVJ",
            playlistUrl = "https://youtube.com/playlist?list=PLKFeMJzFpj5E"
        ),
        YouTubePlaylist(
            id = "PLOTW4vpePh9Y",
            title = "गवाही",
            channelTitle = "Vinay Kumar AVJ",
            playlistUrl = "https://youtube.com/playlist?list=PLOTW4vpePh9Y"
        ),
        YouTubePlaylist(
            id = "PLT-O60XpKwd4",
            title = "Shorts",
            channelTitle = "Vinay Kumar AVJ",
            playlistUrl = "https://youtube.com/playlist?list=PLT-O60XpKwd4"
        ),
        YouTubePlaylist(
            id = "PLFxw_HRKE2Gs",
            title = "News",
            channelTitle = "Vinay Kumar AVJ",
            playlistUrl = "https://youtube.com/playlist?list=PLFxw_HRKE2Gs"
        ),
        YouTubePlaylist(
            id = "PLX2tGei8aSLw",
            title = "अन्य",
            channelTitle = "Vinay Kumar AVJ",
            playlistUrl = "https://youtube.com/playlist?list=PLX2tGei8aSLw"
        )
    )

    val channelMain = YouTubeChannelInfo(
        id = "UClFK75L0wsDMf10Tj77hlsg",
        name = "Vinay Kumar AVJ",
        handle = "@vinaykumaravj",
        channelUrl = "https://youtube.com/@vinaykumaravj",
        description = "Official channel of Vinay Kumar AVJ featuring Fellowship events, messages, songs, and updates.",
        avatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_k..."
    )

    val channelWorship = YouTubeChannelInfo(
        id = "UC92tSCn2I6lwcUyAdyS_MMw",
        name = "Vinay Kumar AVJ Worship",
        handle = "@vinaykumaravjworship",
        channelUrl = "https://youtube.com/@vinaykumaravjworship",
        description = "Worship songs, Hindi Masih Geet, Sadri Christian songs, praises and spiritual melodies.",
        avatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_k..."
    )

    val channelNewCreationChurch = YouTubeChannelInfo(
        id = "UCK4HLm9WeAILv2CfPU9nCmw",
        name = "New Creation Church Ministry",
        handle = "@newcreationchurchministry51015",
        channelUrl = "https://youtube.com/@newcreationchurchministry51015",
        description = "Official channel of New Creation Church Ministry featuring Sermons, Prayers, Worship & Fellowship Services.",
        avatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_k..."
    )

    fun isBlockedYouTubeChannel(channelTitle: String, channelId: String = ""): Boolean {
        if (channelId.isNotBlank() && channelId == channelNewCreationChurch.id) return false
        val title = channelTitle.trim().lowercase()
        // If it explicitly says "Ministry", it's our allowed ministry channel
        if (title.contains("ministry")) return false
        // Block external "New Creation Church" (Singapore / external entity)
        return title == "new creation church" ||
               title.startsWith("new creation church ") ||
               title.startsWith("new creation church -") ||
               title.startsWith("new creation church |")
    }
}
