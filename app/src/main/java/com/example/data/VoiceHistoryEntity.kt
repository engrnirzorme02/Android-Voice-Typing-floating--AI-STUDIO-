package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_history")
data class VoiceHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val language: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val charCount: Int = text.length,
    val wordCount: Int = text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size,
    val isFavorite: Boolean = false
)
