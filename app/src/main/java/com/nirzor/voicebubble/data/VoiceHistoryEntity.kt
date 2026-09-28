package com.nirzor.voicebubble.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_history")
data class VoiceHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "text")
    val text: String,
    @ColumnInfo(name = "language")
    val language: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "durationMs")
    val durationMs: Long = 0L,
    @ColumnInfo(name = "charCount")
    val charCount: Int = text.length,
    @ColumnInfo(name = "wordCount")
    val wordCount: Int = text.trim().split("\\s+".toRegex()).count { it.isNotBlank() },
    @ColumnInfo(name = "isFavorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "rawText", defaultValue = "''")
    val rawText: String = text,
    @ColumnInfo(name = "polishedText", defaultValue = "NULL")
    val polishedText: String? = null,
    @ColumnInfo(name = "createdAt", defaultValue = "0")
    val createdAt: Long = timestamp
)
