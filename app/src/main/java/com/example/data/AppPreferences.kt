package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String
)

object SupportedLanguages {
    val languages = listOf(
        VoiceLanguage("bn-BD", "Bangla (Bangladesh)", "বাংলা (বাংলাদেশ)", "🇧🇩"),
        VoiceLanguage("bn-IN", "Bangla (India)", "বাংলা (ভারত)", "🇮🇳"),
        VoiceLanguage("en-US", "English (United States)", "English (US)", "🇺🇸"),
        VoiceLanguage("en-GB", "English (United Kingdom)", "English (UK)", "🇬🇧"),
        VoiceLanguage("hi-IN", "Hindi (India)", "हिन्दी (भारत)", "🇮🇳"),
        VoiceLanguage("ar-SA", "Arabic (Saudi Arabia)", "العربية", "🇸🇦"),
        VoiceLanguage("es-ES", "Spanish (Spain)", "Español", "🇪🇸"),
        VoiceLanguage("fr-FR", "French (France)", "Français", "🇫🇷"),
        VoiceLanguage("default", "Auto / System Default", "সিস্টেম ডিফল্ট", "🌐")
    )

    fun getLanguageByCode(code: String): VoiceLanguage {
        return languages.find { it.code == code } ?: languages[0] // default to Bangla (Bangladesh)
    }
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("voice_bubble_prefs", Context.MODE_PRIVATE)

    private val _selectedLanguage = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "bn-BD") ?: "bn-BD")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _bubbleSizeDp = MutableStateFlow(prefs.getInt(KEY_BUBBLE_SIZE, 60))
    val bubbleSizeDp: StateFlow<Int> = _bubbleSizeDp.asStateFlow()

    private val _bubbleOpacity = MutableStateFlow(prefs.getFloat(KEY_BUBBLE_OPACITY, 0.95f))
    val bubbleOpacity: StateFlow<Float> = _bubbleOpacity.asStateFlow()

    private val _hapticFeedback = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC, true))
    val hapticFeedback: StateFlow<Boolean> = _hapticFeedback.asStateFlow()

    private val _soundFeedback = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    val soundFeedback: StateFlow<Boolean> = _soundFeedback.asStateFlow()

    private val _autoCopy = MutableStateFlow(prefs.getBoolean(KEY_AUTO_COPY, true))
    val autoCopy: StateFlow<Boolean> = _autoCopy.asStateFlow()

    private val _preferOffline = MutableStateFlow(prefs.getBoolean(KEY_PREFER_OFFLINE, false))
    val preferOffline: StateFlow<Boolean> = _preferOffline.asStateFlow()

    private val _dockToEdge = MutableStateFlow(prefs.getBoolean(KEY_DOCK_TO_EDGE, true))
    val dockToEdge: StateFlow<Boolean> = _dockToEdge.asStateFlow()

    private val _autoDim = MutableStateFlow(prefs.getBoolean(KEY_AUTO_DIM, false))
    val autoDim: StateFlow<Boolean> = _autoDim.asStateFlow()

    private val _bubbleX = MutableStateFlow(prefs.getInt(KEY_BUBBLE_X, -1))
    val bubbleX: StateFlow<Int> = _bubbleX.asStateFlow()

    private val _bubbleY = MutableStateFlow(prefs.getInt(KEY_BUBBLE_Y, -1))
    val bubbleY: StateFlow<Int> = _bubbleY.asStateFlow()

    fun setLanguage(code: String) {
        prefs.edit().putString(KEY_LANGUAGE, code).apply()
        _selectedLanguage.value = code
    }

    fun setBubbleSize(sizeDp: Int) {
        prefs.edit().putInt(KEY_BUBBLE_SIZE, sizeDp).apply()
        _bubbleSizeDp.value = sizeDp
    }

    fun setBubbleOpacity(opacity: Float) {
        prefs.edit().putFloat(KEY_BUBBLE_OPACITY, opacity).apply()
        _bubbleOpacity.value = opacity
    }

    fun setHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _hapticFeedback.value = enabled
    }

    fun setSoundFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _soundFeedback.value = enabled
    }

    fun setAutoCopy(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_COPY, enabled).apply()
        _autoCopy.value = enabled
    }

    fun setPreferOffline(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PREFER_OFFLINE, enabled).apply()
        _preferOffline.value = enabled
    }

    fun setDockToEdge(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DOCK_TO_EDGE, enabled).apply()
        _dockToEdge.value = enabled
    }

    fun setAutoDim(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_DIM, enabled).apply()
        _autoDim.value = enabled
    }

    fun setBubblePosition(x: Int, y: Int) {
        prefs.edit().putInt(KEY_BUBBLE_X, x).putInt(KEY_BUBBLE_Y, y).apply()
        _bubbleX.value = x
        _bubbleY.value = y
    }

    companion object {
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_BUBBLE_SIZE = "key_bubble_size"
        private const val KEY_BUBBLE_OPACITY = "key_bubble_opacity"
        private const val KEY_HAPTIC = "key_haptic"
        private const val KEY_SOUND = "key_sound"
        private const val KEY_AUTO_COPY = "key_auto_copy"
        private const val KEY_PREFER_OFFLINE = "key_prefer_offline"
        private const val KEY_DOCK_TO_EDGE = "key_dock_to_edge"
        private const val KEY_AUTO_DIM = "key_auto_dim"
        private const val KEY_BUBBLE_X = "key_bubble_x"
        private const val KEY_BUBBLE_Y = "key_bubble_y"

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = AppPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
