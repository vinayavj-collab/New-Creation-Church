package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlogPostDao {
    @Query("SELECT * FROM blog_posts WHERE sourceId IN (:sources) ORDER BY publishedTimestamp DESC")
    fun getPostsBySources(sources: List<String>): Flow<List<BlogPostEntity>>

    @Query("SELECT * FROM blog_posts WHERE sourceId = :sourceId ORDER BY publishedTimestamp DESC")
    fun getPostsBySource(sourceId: String): Flow<List<BlogPostEntity>>

    @Query("SELECT * FROM blog_posts WHERE id = :id LIMIT 1")
    fun getPostById(id: String): Flow<BlogPostEntity?>

    @Query("""
        SELECT * FROM blog_posts 
        WHERE sourceId IN (:sources) 
        AND (title LIKE '%' || :query || '%' OR contentHtml LIKE '%' || :query || '%' OR labels LIKE '%' || :query || '%')
        ORDER BY publishedTimestamp DESC
    """)
    fun searchPosts(query: String, sources: List<String>): Flow<List<BlogPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<BlogPostEntity>)

    @Query("DELETE FROM blog_posts WHERE sourceId = :sourceId")
    suspend fun deletePostsBySource(sourceId: String)

    @Query("DELETE FROM blog_posts")
    suspend fun clearAll()
}
