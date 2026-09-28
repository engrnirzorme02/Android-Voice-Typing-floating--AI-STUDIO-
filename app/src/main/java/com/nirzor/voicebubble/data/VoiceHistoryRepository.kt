package com.nirzor.voicebubble.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class VoiceHistoryRepository(
    private val dao: VoiceHistoryDao,
    private val preferences: AppPreferences
) {
    val pagedHistory: Flow<PagingData<VoiceHistoryPreviewDto>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        dao.getPagedHistory()
    }.flow

    val pagedFavoriteHistory: Flow<PagingData<VoiceHistoryPreviewDto>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        dao.getPagedFavoriteHistory()
    }.flow

    val recentHistory: Flow<List<VoiceHistoryEntity>> = dao.getRecentHistory()
    val totalCount: Flow<Int> = dao.getTotalCount()
    val totalWords: Flow<Int?> = dao.getTotalWords()
    val totalChars: Flow<Int?> = dao.getTotalChars()

    fun searchHistoryPreviews(query: String): Flow<List<VoiceHistoryPreviewDto>> =
        dao.searchHistoryPreviews(query)

    suspend fun getById(id: Long): VoiceHistoryEntity? = dao.getById(id)

    suspend fun insert(item: VoiceHistoryEntity): Long {
        val id = dao.insert(item)
        pruneIfRequired()
        return id
    }

    suspend fun update(item: VoiceHistoryEntity) = dao.update(item)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = dao.updateFavorite(id, isFavorite)

    suspend fun pruneIfRequired() {
        val retentionDays = preferences.historyRetentionDays.value
        if (retentionDays > 0) {
            val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24L * 60L * 60L * 1000L)
            dao.pruneOlderThan(cutoff)
        }
    }
}
