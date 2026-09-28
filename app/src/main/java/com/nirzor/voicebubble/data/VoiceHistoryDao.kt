package com.nirzor.voicebubble.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceHistoryDao {

    @Query("""
        SELECT id, SUBSTR(text, 1, 200) AS previewText, rawText, polishedText, language, createdAt, isFavorite, wordCount, charCount 
        FROM voice_history 
        ORDER BY createdAt DESC
    """)
    fun getPagedHistory(): PagingSource<Int, VoiceHistoryPreviewDto>

    @Query("""
        SELECT id, SUBSTR(text, 1, 200) AS previewText, rawText, polishedText, language, createdAt, isFavorite, wordCount, charCount 
        FROM voice_history 
        WHERE isFavorite = 1 
        ORDER BY createdAt DESC
    """)
    fun getPagedFavoriteHistory(): PagingSource<Int, VoiceHistoryPreviewDto>

    @Query("""
        SELECT id, SUBSTR(text, 1, 200) AS previewText, rawText, polishedText, language, createdAt, isFavorite, wordCount, charCount 
        FROM voice_history 
        WHERE text LIKE '%' || :query || '%' 
           OR rawText LIKE '%' || :query || '%' 
           OR (polishedText IS NOT NULL AND polishedText LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchHistoryPreviews(query: String): Flow<List<VoiceHistoryPreviewDto>>

    @Query("SELECT * FROM voice_history WHERE id = :id")
    suspend fun getById(id: Long): VoiceHistoryEntity?

    @Query("SELECT * FROM voice_history ORDER BY createdAt DESC LIMIT 5")
    fun getRecentHistory(): Flow<List<VoiceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VoiceHistoryEntity): Long

    @Update
    suspend fun update(item: VoiceHistoryEntity)

    @Query("DELETE FROM voice_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM voice_history")
    suspend fun clearAll()

    @Query("UPDATE voice_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM voice_history")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT SUM(wordCount) FROM voice_history")
    fun getTotalWords(): Flow<Int?>

    @Query("SELECT SUM(charCount) FROM voice_history")
    fun getTotalChars(): Flow<Int?>

    @Query("DELETE FROM voice_history WHERE createdAt < :cutoffTimestamp")
    suspend fun pruneOlderThan(cutoffTimestamp: Long): Int
}
