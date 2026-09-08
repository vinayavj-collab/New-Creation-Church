package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.SavedItemEntity
import kotlinx.coroutines.flow.Flow

class SavedItemRepository(private val database: AppDatabase) {
    private val dao = database.savedItemDao()

    fun getAllSavedItems(): Flow<List<SavedItemEntity>> = dao.getAllSavedItems()

    fun getSavedItemsByType(type: String): Flow<List<SavedItemEntity>> = dao.getSavedItemsByType(type)

    fun isSavedFlow(id: String): Flow<Boolean> = dao.isItemSaved(id)

    suspend fun save(item: SavedItemEntity) {
        dao.saveItem(item)
    }

    suspend fun remove(id: String) {
        dao.removeItem(id)
    }

    suspend fun toggleSave(item: SavedItemEntity, isCurrentlySaved: Boolean) {
        if (isCurrentlySaved) {
            dao.removeItem(item.id)
        } else {
            dao.saveItem(item)
        }
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
