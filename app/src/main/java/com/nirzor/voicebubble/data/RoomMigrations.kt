package com.nirzor.voicebubble.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE voice_history ADD COLUMN rawText TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE voice_history ADD COLUMN polishedText TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE voice_history ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE voice_history SET rawText = text WHERE rawText = ''")
        db.execSQL("UPDATE voice_history SET createdAt = timestamp WHERE createdAt = 0")
    }
}
