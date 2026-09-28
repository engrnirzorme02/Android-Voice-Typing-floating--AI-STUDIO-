package com.nirzor.voicebubble.speech

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface BubbleState {
    object Idle : BubbleState
    data class Listening(val rmsDb: Float = 0f, val partialText: String = "") : BubbleState
    object Processing : BubbleState
    data class Done(val rawText: String, val polishedText: String?, val copied: Boolean = true) : BubbleState
    data class Error(val message: String, val errorCode: Int) : BubbleState
}

class BubbleStateMachine {

    private val _state = MutableStateFlow<BubbleState>(BubbleState.Idle)
    val state: StateFlow<BubbleState> = _state.asStateFlow()

    fun canAcceptTap(): Boolean {
        return _state.value is BubbleState.Idle
    }

    fun startListening(): Boolean {
        if (_state.value !is BubbleState.Idle) {
            return false // Taps in non-IDLE states are ignored
        }
        _state.value = BubbleState.Listening(0f, "")
        return true
    }

    fun updateRms(rms: Float) {
        val current = _state.value
        if (current is BubbleState.Listening) {
            _state.value = current.copy(rmsDb = rms)
        }
    }

    fun updatePartialText(partial: String) {
        val current = _state.value
        if (current is BubbleState.Listening) {
            _state.value = current.copy(partialText = partial)
        }
    }

    fun moveToProcessing(): Boolean {
        if (_state.value is BubbleState.Listening) {
            _state.value = BubbleState.Processing
            return true
        }
        return false
    }

    fun moveToDone(rawText: String, polishedText: String?, copied: Boolean = true): Boolean {
        if (_state.value is BubbleState.Processing || _state.value is BubbleState.Listening) {
            _state.value = BubbleState.Done(rawText, polishedText, copied)
            return true
        }
        return false
    }

    fun moveToError(message: String, errorCode: Int) {
        _state.value = BubbleState.Error(message, errorCode)
    }

    fun resetToIdle() {
        _state.value = BubbleState.Idle
    }
}
