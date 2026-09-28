package com.nirzor.voicebubble

import android.app.Application
import com.nirzor.voicebubble.data.AppDatabase
import com.nirzor.voicebubble.data.AppPreferences
import com.nirzor.voicebubble.data.VoiceHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        preferences = AppPreferences.getInstance(this)
        database = AppDatabase.getDatabase(this)
        repository = VoiceHistoryRepository(database.voiceHistoryDao(), preferences)

        // Phase D.3: Auto-prune records older than retention setting on app open
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.pruneIfRequired()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    companion object {
        lateinit var instance: VoiceBubbleApp
            private set
    }
}
