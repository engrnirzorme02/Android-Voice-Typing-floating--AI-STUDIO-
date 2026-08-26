package com.example.speech

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed interface SpeechState {
    object Idle : SpeechState
    data class Listening(val rmsDb: Float = 0f, val partialText: String = "") : SpeechState
    object Processing : SpeechState
    data class Success(val text: String, val language: String, val durationMs: Long) : SpeechState
    data class Error(val message: String, val errorCode: Int, val isNetworkIssue: Boolean = false) : SpeechState
}

class SpeechEngine(private val context: Context) {

    private val TAG = "SpeechEngine"
    private var speechRecognizer: SpeechRecognizer? = null
    private var startTimeMs: Long = 0L

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _currentRms = MutableStateFlow(0f)
    val currentRms: StateFlow<Float> = _currentRms.asStateFlow()

    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize ToneGenerator", e)
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(
        languageCode: String = "bn-BD",
        preferOffline: Boolean = false,
        playTone: Boolean = true,
        enableHaptic: Boolean = true,
        onPartialResult: ((String) -> Unit)? = null,
        onFinalResult: ((String, String, Long) -> Unit)? = null,
        onErrorOccurred: ((String, Boolean) -> Unit)? = null
    ) {
        stopListening()

        if (!isRecognitionAvailable()) {
            val errorMsg = "Speech recognition is not available on this device. Please install or enable Google Speech Services."
            _speechState.value = SpeechState.Error(errorMsg, -1, false)
            onErrorOccurred?.invoke(errorMsg, false)
            return
        }

        if (enableHaptic) {
            triggerHaptic(50)
        }
        if (playTone) {
            playBeep(ToneGenerator.TONE_PROP_BEEP)
        }

        startTimeMs = System.currentTimeMillis()
        _speechState.value = SpeechState.Listening(0f, "")
        _currentRms.value = 0f

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            if (languageCode != "default") {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageCode)
            } else {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            }

            if (preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "onReadyForSpeech")
                        _speechState.value = SpeechState.Listening(0f, "")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _currentRms.value = normalized
                        val current = _speechState.value
                        if (current is SpeechState.Listening) {
                            _speechState.value = current.copy(rmsDb = normalized)
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "onEndOfSpeech")
                        _speechState.value = SpeechState.Processing
                    }

                    override fun onError(error: Int) {
                        Log.e(TAG, "Speech recognition error: $error")
                        val isNetworkIssue = (error == SpeechRecognizer.ERROR_NETWORK ||
                                error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                                error == SpeechRecognizer.ERROR_SERVER)

                        val errorMsg = getErrorMessage(error)
                        _speechState.value = SpeechState.Error(errorMsg, error, isNetworkIssue)
                        if (enableHaptic) {
                            triggerHapticDouble()
                        }
                        onErrorOccurred?.invoke(errorMsg, isNetworkIssue)
                    }

                    override fun onResults(results: Bundle?) {
                        val duration = System.currentTimeMillis() - startTimeMs
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: ""

                        if (text.isNotBlank()) {
                            if (enableHaptic) {
                                triggerHapticSuccess()
                            }
                            if (playTone) {
                                playBeep(ToneGenerator.TONE_PROP_ACK)
                            }
                            _speechState.value = SpeechState.Success(text, languageCode, duration)
                            onFinalResult?.invoke(text, languageCode, duration)
                        } else {
                            val msg = "কোনো কথা শনাক্ত করা যায়নি (No speech detected)"
                            _speechState.value = SpeechState.Error(msg, SpeechRecognizer.ERROR_NO_MATCH, false)
                            onErrorOccurred?.invoke(msg, false)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim() ?: ""
                        if (partial.isNotBlank()) {
                            _speechState.value = SpeechState.Listening(_currentRms.value, partial)
                            onPartialResult?.invoke(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                startListening(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognizer", e)
            val errorMsg = "রেকর্ডিং শুরু করতে সমস্যা হয়েছে: ${e.localizedMessage}"
            _speechState.value = SpeechState.Error(errorMsg, -1, false)
            onErrorOccurred?.invoke(errorMsg, false)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up speech recognizer", e)
        }
        _currentRms.value = 0f
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling speech recognizer", e)
        }
        _speechState.value = SpeechState.Idle
        _currentRms.value = 0f
    }

    private fun playBeep(toneType: Int) {
        try {
            toneGenerator?.startTone(toneType, 120)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play beep", e)
        }
    }

    private fun triggerHaptic(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic error", e)
        }
    }

    private fun triggerHapticSuccess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 40, 60, 80)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 40, 60, 80), -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic error", e)
        }
    }

    private fun triggerHapticDouble() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 60, 80, 60)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 60, 80, 60), -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic error", e)
        }
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "অডিও রেকর্ডিং এরর (Audio recording error)"
            SpeechRecognizer.ERROR_CLIENT -> "ক্লায়েন্ট সাইড এরর (Client error)"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন পারমিশন প্রয়োজন (Microphone permission required)"
            SpeechRecognizer.ERROR_NETWORK -> "নেটওয়ার্ক সমস্যা! আপনার ডিভাইসে অফলাইন স্পিচ ডাটা ডাউনলোড করুন বা মোবাইল ডাটা ট্রাই করুন।"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "নেটওয়ার্ক টাইমআউট হয়েছে। আবার চেষ্টা করুন।"
            SpeechRecognizer.ERROR_NO_MATCH -> "কোনো কথা শনাক্ত করা যায়নি (No match found)"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "স্পিচ সার্ভিস ব্যস্ত রয়েছে, অনুগ্রহ করে পুনরায় চেষ্টা করুন।"
            SpeechRecognizer.ERROR_SERVER -> "সার্ভার এরর (Google Speech Server error)"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কথা শনাক্তের সময়সীমা পার হয়ে গেছে (Speech timeout)"
            else -> "ভয়েস টাইপিং ত্রুটি ($errorCode)"
        }
    }
}
