package com.example.data.model

enum class SearchResultType(val label: String) {
    BLOG("BLOG"),
    VIDEO("VIDEO"),
    PLAYLIST("PLAYLIST"),
    BIBLE("BIBLE")
}

data class SearchResultItem(
    val id: String,
    val type: SearchResultType,
    val title: String,
    val snippet: String,
    val imageUrl: String? = null,
    val post: BlogPost? = null,
    val video: YouTubeVideo? = null,
    val playlist: YouTubePlaylist? = null,
    val bibleBookId: String? = null,
    val bibleChapter: Int? = null,
    val bibleVerse: Int? = null
)
