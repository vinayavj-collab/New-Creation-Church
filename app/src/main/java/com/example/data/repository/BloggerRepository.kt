package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.BlogPostEntity
import com.example.data.model.BlogPost
import com.example.data.model.BlogSourceType
import com.example.data.model.GalleryPhoto
import com.example.data.remote.BloggerFeedService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BloggerRepository(
    private val database: AppDatabase,
    private val feedService: BloggerFeedService = BloggerFeedService()
) {
    private val dao = database.blogPostDao()

    fun getPostsFlow(showPersonalVlog: Boolean): Flow<List<BlogPost>> {
        val sources = if (showPersonalVlog) {
            listOf(BlogSourceType.FELLOWSHIP_EVENTS.id, BlogSourceType.PERSONAL_VLOG.id)
        } else {
            listOf(BlogSourceType.FELLOWSHIP_EVENTS.id)
        }
        return dao.getPostsBySources(sources).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getPostsBySourceFlow(source: BlogSourceType): Flow<List<BlogPost>> {
        return dao.getPostsBySource(source.id).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getPostById(id: String): Flow<BlogPost?> {
        return dao.getPostById(id).map { it?.toDomain() }
    }

    fun searchPosts(query: String, showPersonalVlog: Boolean): Flow<List<BlogPost>> {
        val sources = if (showPersonalVlog) {
            listOf(BlogSourceType.FELLOWSHIP_EVENTS.id, BlogSourceType.PERSONAL_VLOG.id)
        } else {
            listOf(BlogSourceType.FELLOWSHIP_EVENTS.id)
        }
        return dao.searchPosts(query, sources).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun refreshPosts(showPersonalVlog: Boolean): Result<Unit> {
        return try {
            val fellowshipPosts = feedService.fetchBlogPosts(BlogSourceType.FELLOWSHIP_EVENTS)
            if (fellowshipPosts.isNotEmpty()) {
                dao.insertPosts(fellowshipPosts.map { BlogPostEntity.fromDomain(it) })
            }

            if (showPersonalVlog) {
                val personalPosts = feedService.fetchBlogPosts(BlogSourceType.PERSONAL_VLOG)
                if (personalPosts.isNotEmpty()) {
                    dao.insertPosts(personalPosts.map { BlogPostEntity.fromDomain(it) })
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCache() {
        dao.clearAll()
    }

    fun extractPhotos(posts: List<BlogPost>): List<GalleryPhoto> {
        val photos = mutableListOf<GalleryPhoto>()
        posts.forEach { post ->
            post.allImages.forEach { imgUrl ->
                photos.add(
                    GalleryPhoto(
                        imageUrl = imgUrl,
                        postTitle = post.title,
                        postId = post.id,
                        source = post.source,
                        publishedDate = post.publishedDate
                    )
                )
            }
        }
        return photos
    }
}
