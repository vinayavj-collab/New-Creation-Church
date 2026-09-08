package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recently_viewed")
data class RecentlyViewedEntity(
    @PrimaryKey val id: String,
    val type: String, // BLOG, VIDEO, PLAYLIST, BIBLE
    val title: String,
    val subtitle: String,
    val imageUrl: String? = null,
    val extraDataJson: String? = null,
    val viewedTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface RecentlyViewedDao {
    @Query("SELECT * FROM recently_viewed ORDER BY viewedTimestamp DESC LIMIT 30")
    fun getRecentItems(): Flow<List<RecentlyViewedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordItem(item: RecentlyViewedEntity)

    @Query("DELETE FROM recently_viewed WHERE id = :id")
    suspend fun removeItem(id: String)

    @Query("DELETE FROM recently_viewed")
    suspend fun clearHistory()
}
