package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.RecentlyViewedEntity
import kotlinx.coroutines.flow.Flow

class RecentlyViewedRepository(private val database: AppDatabase) {
    private val dao = database.recentlyViewedDao()

    fun getRecentItems(): Flow<List<RecentlyViewedEntity>> = dao.getRecentItems()

    suspend fun record(
        id: String,
        type: String,
        title: String,
        subtitle: String,
        imageUrl: String? = null,
        extraDataJson: String? = null
    ) {
        dao.recordItem(
            RecentlyViewedEntity(
                id = id,
                type = type,
                title = title,
                subtitle = subtitle,
                imageUrl = imageUrl,
                extraDataJson = extraDataJson,
                viewedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun remove(id: String) {
        dao.removeItem(id)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }
}
