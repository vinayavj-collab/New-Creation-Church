package com.example.data.model

sealed class MixedFeedItem {
    data class BlogPostItem(val post: BlogPost) : MixedFeedItem()
    data class VideoItem(val video: YouTubeVideo) : MixedFeedItem()
}
