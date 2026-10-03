package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleSearchHistoryDao {
    @Query("SELECT * FROM bible_search_history ORDER BY timestamp DESC LIMIT 20")
    fun getSearchHistory(): Flow<List<BibleSearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(item: BibleSearchHistoryEntity)

    @Query("DELETE FROM bible_search_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Long)

    @Query("DELETE FROM bible_search_history")
    suspend fun clearHistory()
}
