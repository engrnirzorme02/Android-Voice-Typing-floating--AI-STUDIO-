package com.nirzor.voicebubble.data

data class VoiceHistoryPreviewDto(
    val id: Long,
    val previewText: String,
    val rawText: String,
    val polishedText: String?,
    val language: String,
    val createdAt: Long,
    val isFavorite: Boolean,
    val wordCount: Int,
    val charCount: Int
)
