package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.BlogPostEntity
import com.example.data.local.PredefinedData
import com.example.data.model.BlogPost
import com.example.data.model.BlogSourceType
import com.example.data.model.GalleryPhoto
import com.example.data.remote.BloggerFeedService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class BloggerRepository(
    private val database: AppDatabase,
    private val feedService: BloggerFeedService = BloggerFeedService()
) {
    private val dao = database.blogPostDao()
    private val firebaseDataRepository by lazy { FirebaseDataRepository.getInstance() }

    fun getPostsFlow(showPersonalVlog: Boolean): Flow<List<BlogPost>> {
        val sources = if (showPersonalVlog) {
            listOf(BlogSourceType.FELLOWSHIP_EVENTS.id, BlogSourceType.PERSONAL_VLOG.id)
        } else {
            listOf(BlogSourceType.FELLOWSHIP_EVENTS.id)
        }
        val remoteFlow = if (showPersonalVlog) firebaseDataRepository.allBlogs else firebaseDataRepository.fellowshipBlogs

        return combine(dao.getPostsBySources(sources), remoteFlow) { entities, remoteMerged ->
            val dbPosts = entities.map { it.toDomain() }
            val localHardcoded = if (showPersonalVlog) {
                PredefinedData.hardcodedFellowshipBlogs + PredefinedData.hardcodedPersonalVlogs
            } else {
                PredefinedData.hardcodedFellowshipBlogs
            }
            FirebaseDataRepository.mergeAndDeduplicate(
                remoteItems = remoteMerged,
                localHardcodedItems = localHardcoded + dbPosts,
                keySelector = { it.id.ifBlank { it.url } },
                timestampSelector = { it.publishedTimestamp }
            )
        }
    }

    fun getPostsBySourceFlow(source: BlogSourceType): Flow<List<BlogPost>> {
        val remoteFlow = when (source) {
            BlogSourceType.FELLOWSHIP_EVENTS -> firebaseDataRepository.fellowshipBlogs
            BlogSourceType.PERSONAL_VLOG -> firebaseDataRepository.personalVlogs
        }
        val hardcoded = when (source) {
            BlogSourceType.FELLOWSHIP_EVENTS -> PredefinedData.hardcodedFellowshipBlogs
            BlogSourceType.PERSONAL_VLOG -> PredefinedData.hardcodedPersonalVlogs
        }
        return combine(dao.getPostsBySource(source.id), remoteFlow) { entities, remoteMerged ->
            val dbPosts = entities.map { it.toDomain() }
            FirebaseDataRepository.mergeAndDeduplicate(
                remoteItems = remoteMerged,
                localHardcodedItems = hardcoded + dbPosts,
                keySelector = { it.id.ifBlank { it.url } },
                timestampSelector = { it.publishedTimestamp }
            )
        }
    }

    fun getPostById(id: String): Flow<BlogPost?> {
        return getPostsFlow(showPersonalVlog = true).map { list ->
            list.find { it.id == id }
        }
    }

    fun searchPosts(query: String, showPersonalVlog: Boolean): Flow<List<BlogPost>> {
        val q = query.trim().lowercase()
        return getPostsFlow(showPersonalVlog).map { list ->
            if (q.isBlank()) list
            else list.filter { post ->
                post.title.lowercase().contains(q) ||
                    post.plainTextExcerpt.lowercase().contains(q) ||
                    post.labels.any { it.lowercase().contains(q) }
            }
        }
    }

    suspend fun refreshPosts(showPersonalVlog: Boolean): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val fellowshipPosts = feedService.fetchBlogPosts(BlogSourceType.FELLOWSHIP_EVENTS)
            if (fellowshipPosts.isNotEmpty()) {
                dao.insertPosts(fellowshipPosts.map { BlogPostEntity.fromDomain(it) })
            }

            val shouldFetchPersonal = showPersonalVlog || com.example.util.ProfileManager.isVinayProfile()
            if (shouldFetchPersonal) {
                try {
                    val personalPosts = feedService.fetchBlogPosts(BlogSourceType.PERSONAL_VLOG)
                    if (personalPosts.isNotEmpty()) {
                        dao.insertPosts(personalPosts.map { BlogPostEntity.fromDomain(it) })
                    }
                } catch (e: Exception) {
                    // Ignored
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCache() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
