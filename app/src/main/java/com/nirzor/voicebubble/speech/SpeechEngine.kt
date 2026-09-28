package com.nirzor.voicebubble.speech

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.nirzor.voicebubble.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SpeechEngine(private val context: Context) {

    private val TAG = "SpeechEngine"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    val stateMachine = BubbleStateMachine()
    val speechState: StateFlow<BubbleState> = stateMachine.state

    private var speechRecognizer: SpeechRecognizer? = null
    private var startTimeMs: Long = 0L
    private var hasRetriedBusy = false
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator init failed", e)
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

    fun startSession(
        languageCode: String = "bn-BD",
        playTone: Boolean = true,
        enableHaptic: Boolean = true,
        onPartialResult: ((String) -> Unit)? = null,
        onFinalResult: ((rawText: String, lang: String, durationMs: Long) -> Unit)? = null,
        onErrorOccurred: ((errorMessage: String, errorCode: Int) -> Unit)? = null
    ) {
        if (!stateMachine.canAcceptTap()) {
            Log.d(TAG, "Taps ignored while not in IDLE state")
            return
        }

        if (!isRecognitionAvailable()) {
            val errorMsg = context.getString(R.string.speech_not_available)
            stateMachine.moveToError(errorMsg, -1)
            onErrorOccurred?.invoke(errorMsg, -1)
            return
        }

        hasRetriedBusy = false

        // Cancel & destroy old recognizer on main looper, wait ~300ms, then launch new session
        cleanupRecognizer()

        scope.launch {
            delay(300L)
            internalStartRecognizer(
                languageCode = languageCode,
                playTone = playTone,
                enableHaptic = enableHaptic,
                onPartialResult = onPartialResult,
                onFinalResult = onFinalResult,
                onErrorOccurred = onErrorOccurred
            )
        }
    }

    private fun internalStartRecognizer(
        languageCode: String,
        playTone: Boolean,
        enableHaptic: Boolean,
        onPartialResult: ((String) -> Unit)?,
        onFinalResult: ((rawText: String, lang: String, durationMs: Long) -> Unit)?,
        onErrorOccurred: ((errorMessage: String, errorCode: Int) -> Unit)?
    ) {
        mainHandler.post {
            if (!stateMachine.startListening()) {
                return@post
            }

            if (enableHaptic) triggerHaptic(50)
            if (playTone) playBeep(ToneGenerator.TONE_PROP_BEEP)

            startTimeMs = System.currentTimeMillis()

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
            }

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d(TAG, "SpeechRecognizer onReadyForSpeech")
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d(TAG, "SpeechRecognizer onBeginningOfSpeech")
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            stateMachine.updateRms(normalized)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            Log.d(TAG, "SpeechRecognizer onEndOfSpeech")
                            stateMachine.moveToProcessing()
                        }

                        override fun onError(error: Int) {
                            Log.e(TAG, "SpeechRecognizer onError: $error")

                            // Retry once on ERROR_RECOGNIZER_BUSY (error 8)
                            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY && !hasRetriedBusy) {
                                hasRetriedBusy = true
                                cleanupRecognizer()
                                scope.launch {
                                    delay(300L)
                                    internalStartRecognizer(
                                        languageCode, playTone, enableHaptic,
                                        onPartialResult, onFinalResult, onErrorOccurred
                                    )
                                }
                                return
                            }

                            val errorMsg = getErrorMessage(error)
                            stateMachine.moveToError(errorMsg, error)
                            if (enableHaptic) triggerHapticDouble()
                            onErrorOccurred?.invoke(errorMsg, error)
                        }

                        override fun onResults(results: Bundle?) {
                            val duration = System.currentTimeMillis() - startTimeMs
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""

                            if (text.isNotBlank()) {
                                if (enableHaptic) triggerHapticSuccess()
                                if (playTone) playBeep(ToneGenerator.TONE_PROP_ACK)
                                onFinalResult?.invoke(text, languageCode, duration)
                            } else {
                                val msg = context.getString(R.string.speech_err_no_match)
                                stateMachine.moveToError(msg, SpeechRecognizer.ERROR_NO_MATCH)
                                onErrorOccurred?.invoke(msg, SpeechRecognizer.ERROR_NO_MATCH)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull()?.trim() ?: ""
                            if (partial.isNotBlank()) {
                                stateMachine.updatePartialText(partial)
                                onPartialResult?.invoke(partial)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                    startListening(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognizer", e)
                val errorMsg = context.getString(R.string.speech_err_unknown, -1)
                stateMachine.moveToError(errorMsg, -1)
                onErrorOccurred?.invoke(errorMsg, -1)
            }
        }
    }

    fun stopSession() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping speech recognizer", e)
            }
        }
    }

    fun cancelSession() {
        mainHandler.post {
            cleanupRecognizer()
            stateMachine.resetToIdle()
        }
    }

    fun destroy() {
        mainHandler.post {
            cleanupRecognizer()
            stateMachine.resetToIdle()
            try {
                toneGenerator?.release()
                toneGenerator = null
            } catch (e: Exception) {
                Log.w(TAG, "ToneGenerator release error", e)
            }
        }
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up SpeechRecognizer", e)
        }
    }

    private fun playBeep(toneType: Int) {
        try {
            toneGenerator?.startTone(toneType, 120)
        } catch (e: Exception) {
            Log.w(TAG, "Tone play error", e)
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
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> context.getString(R.string.speech_err_network_timeout)
            SpeechRecognizer.ERROR_NETWORK -> context.getString(R.string.speech_err_network)
            SpeechRecognizer.ERROR_AUDIO -> context.getString(R.string.speech_err_audio)
            SpeechRecognizer.ERROR_SERVER -> context.getString(R.string.speech_err_server)
            SpeechRecognizer.ERROR_CLIENT -> context.getString(R.string.speech_err_client)
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> context.getString(R.string.speech_err_speech_timeout)
            SpeechRecognizer.ERROR_NO_MATCH -> context.getString(R.string.speech_err_no_match)
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> context.getString(R.string.speech_err_busy_final)
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> context.getString(R.string.speech_err_permissions)
            else -> context.getString(R.string.speech_err_unknown, errorCode)
        }
    }
}
