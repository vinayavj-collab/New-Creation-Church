package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_items")
data class SavedItemEntity(
    @PrimaryKey val id: String,
    val type: String, // BLOG, VIDEO, PLAYLIST, BIBLE
    val title: String,
    val subtitle: String,
    val imageUrl: String? = null,
    val url: String? = null,
    val extraDataJson: String? = null,
    val savedTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface SavedItemDao {
    @Query("SELECT * FROM saved_items ORDER BY savedTimestamp DESC")
    fun getAllSavedItems(): Flow<List<SavedItemEntity>>

    @Query("SELECT * FROM saved_items WHERE type = :type ORDER BY savedTimestamp DESC")
    fun getSavedItemsByType(type: String): Flow<List<SavedItemEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_items WHERE id = :id)")
    fun isItemSaved(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveItem(item: SavedItemEntity)

    @Query("DELETE FROM saved_items WHERE id = :id")
    suspend fun removeItem(id: String)

    @Query("DELETE FROM saved_items")
    suspend fun clearAll()
}
