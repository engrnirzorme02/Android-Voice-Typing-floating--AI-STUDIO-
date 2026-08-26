package com.example.data

import kotlinx.coroutines.flow.Flow

class VoiceHistoryRepository(private val dao: VoiceHistoryDao) {
    val allHistory: Flow<List<VoiceHistoryEntity>> = dao.getAllHistory()
    val favoriteHistory: Flow<List<VoiceHistoryEntity>> = dao.getFavoriteHistory()
    val totalCount: Flow<Int> = dao.getTotalCount()

    fun searchHistory(query: String): Flow<List<VoiceHistoryEntity>> = dao.searchHistory(query)

    suspend fun insert(item: VoiceHistoryEntity): Long = dao.insert(item)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = dao.updateFavorite(id, isFavorite)
}
