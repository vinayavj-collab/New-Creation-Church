package com.example.data.repository

import com.example.data.local.BibleSearchHistoryDao
import com.example.data.local.BibleSearchHistoryEntity
import kotlinx.coroutines.flow.Flow

class BibleSearchHistoryRepository(private val dao: BibleSearchHistoryDao) {
    val searchHistory: Flow<List<BibleSearchHistoryEntity>> = dao.getSearchHistory()

    suspend fun saveSearch(query: String) {
        if (query.isNotBlank()) {
            dao.insertSearchQuery(BibleSearchHistoryEntity(query = query.trim()))
        }
    }

    suspend fun deleteItem(id: Long) = dao.deleteHistoryItem(id)

    suspend fun clearAll() = dao.clearHistory()
}
