package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.AppPreferences
import com.example.data.VoiceHistoryRepository

class VoiceBubbleApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: VoiceHistoryRepository
        private set

    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        repository = VoiceHistoryRepository(database.voiceHistoryDao())
        preferences = AppPreferences.getInstance(this)
    }

    companion object {
        lateinit var instance: VoiceBubbleApp
            private set
    }
}
