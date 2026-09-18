package com.example.data.model

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
    val isRemote: Boolean = false
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
        name = "New Creation Church",
        handle = "@newcreationchurchministry51015",
        channelUrl = "https://youtube.com/@newcreationchurchministry51015",
        description = "Official channel of New Creation Church Ministry featuring Sermons, Prayers, Worship & Fellowship Services.",
        avatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_k..."
    )

    val channelDailymotionChristian = YouTubeChannelInfo(
        id = "dm_x27lzjr",
        name = "Christian Media (Dailymotion)",
        handle = "@x27lzjr",
        channelUrl = "https://www.dailymotion.com/partner/x27lzjr/media/video",
        description = "Christian songs, sermons, worship, and spiritual media broadcasted via Dailymotion.",
        avatarUrl = "https://www.dailymotion.com/thumbnail/user/x27lzjr"
    )

    val channelDailymotionVlog = YouTubeChannelInfo(
        id = "dm_x4sr8o4",
        name = "Personal Vlog (Dailymotion)",
        handle = "@x4sr8o4",
        channelUrl = "https://www.dailymotion.com/partner/x4sr8o4/media/video",
        description = "Pastor Vinay's Personal Vlogs and reflections on Dailymotion.",
        avatarUrl = "https://www.dailymotion.com/thumbnail/user/x4sr8o4"
    )
}
