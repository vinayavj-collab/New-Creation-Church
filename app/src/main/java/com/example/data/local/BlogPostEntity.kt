package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BlogPost
import com.example.data.model.BlogSourceType

@Entity(tableName = "blog_posts")
data class BlogPostEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val title: String,
    val publishedDate: String,
    val publishedTimestamp: Long,
    val labels: List<String>,
    val featuredImageUrl: String?,
    val allImages: List<String>,
    val plainTextExcerpt: String,
    val contentHtml: String,
    val url: String,
    val embeddedVideoIds: List<String>
) {
    fun toDomain(): BlogPost {
        return BlogPost(
            id = id,
            source = BlogSourceType.fromId(sourceId),
            title = title,
            publishedDate = publishedDate,
            publishedTimestamp = publishedTimestamp,
            labels = labels,
            featuredImageUrl = featuredImageUrl,
            allImages = allImages,
            plainTextExcerpt = plainTextExcerpt,
            contentHtml = contentHtml,
            url = url,
            embeddedVideoIds = embeddedVideoIds
        )
    }

    companion object {
        fun fromDomain(post: BlogPost): BlogPostEntity {
            return BlogPostEntity(
                id = post.id,
                sourceId = post.source.id,
                title = post.title,
                publishedDate = post.publishedDate,
                publishedTimestamp = post.publishedTimestamp,
                labels = post.labels,
                featuredImageUrl = post.featuredImageUrl,
                allImages = post.allImages,
                plainTextExcerpt = post.plainTextExcerpt,
                contentHtml = post.contentHtml,
                url = post.url,
                embeddedVideoIds = post.embeddedVideoIds
            )
        }
    }
}
