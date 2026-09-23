package com.example.data.model

enum class BlogSourceType(val id: String, val displayName: String, val homeUrl: String) {
    FELLOWSHIP_EVENTS(
        id = "fellowship_events",
        displayName = "फेलोशिप",
        homeUrl = "https://vinaykumaravj.blogspot.com/"
    ),
    PERSONAL_VLOG(
        id = "personal_vlog",
        displayName = "पर्सनल लाइफ़",
        homeUrl = "https://vinayavj.blogspot.com/"
    );

    companion object {
        fun fromId(id: String): BlogSourceType {
            return entries.find { it.id == id } ?: FELLOWSHIP_EVENTS
        }
    }
}

data class BlogPost(
    val id: String,
    val source: BlogSourceType,
    val title: String,
    val publishedDate: String,
    val publishedTimestamp: Long,
    val labels: List<String>,
    val featuredImageUrl: String?,
    val allImages: List<String>,
    val plainTextExcerpt: String,
    val contentHtml: String,
    val url: String,
    val embeddedVideoIds: List<String> = emptyList()
)

data class GalleryPhoto(
    val imageUrl: String,
    val postTitle: String,
    val postId: String,
    val source: BlogSourceType,
    val publishedDate: String
)
