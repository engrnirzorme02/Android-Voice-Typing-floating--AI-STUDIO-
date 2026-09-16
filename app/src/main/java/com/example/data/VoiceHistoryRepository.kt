package com.example.data

import kotlinx.coroutines.flow.Flow

class VoiceHistoryRepository(private val dao: VoiceHistoryDao) {
    companion object {
        const val MAX_HISTORY_ENTRIES = 500
    }
    val allHistory: Flow<List<VoiceHistoryEntity>> = dao.getAllHistory()
    val favoriteHistory: Flow<List<VoiceHistoryEntity>> = dao.getFavoriteHistory()
    val totalCount: Flow<Int> = dao.getTotalCount()

    fun searchHistory(query: String): Flow<List<VoiceHistoryEntity>> = dao.searchHistory(query)

    suspend fun insert(item: VoiceHistoryEntity): Long {
        return dao.insertAndTrim(item, MAX_HISTORY_ENTRIES)
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = dao.updateFavorite(id, isFavorite)
}
