package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceHistoryDao {
    @Query("SELECT * FROM voice_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<VoiceHistoryEntity>>

    @Query("SELECT * FROM voice_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteHistory(): Flow<List<VoiceHistoryEntity>>

    @Query("SELECT * FROM voice_history WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<VoiceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VoiceHistoryEntity): Long

    @Query("DELETE FROM voice_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM voice_history")
    suspend fun clearAll()

    @Query("UPDATE voice_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM voice_history")
    fun getTotalCount(): Flow<Int>
}
