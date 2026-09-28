package com.example.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

data class VoiceInputItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class UndoResult(
    val success: Boolean,
    val restoredText: String?,
    val undoneText: String?,
    val userMessage: String
)

data class RedoResult(
    val success: Boolean,
    val restoredText: String?,
    val userMessage: String
)

object UndoRedoManager {
    private const val MAX_HISTORY = 30

    private val undoStack = ArrayDeque<VoiceInputItem>()
    private val redoStack = ArrayDeque<VoiceInputItem>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _currentText = MutableStateFlow<String?>(null)
    val currentText: StateFlow<String?> = _currentText.asStateFlow()

    private val _lastUndoneText = MutableStateFlow<String?>(null)
    val lastUndoneText: StateFlow<String?> = _lastUndoneText.asStateFlow()

    private val _totalRecordedCount = MutableStateFlow(0)
    val totalRecordedCount: StateFlow<Int> = _totalRecordedCount.asStateFlow()

    @Synchronized
    fun recordInput(context: Context, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        // Avoid pushing identical consecutive inputs
        if (undoStack.isNotEmpty() && undoStack.peekLast()?.text == trimmed) {
            return
        }

        if (undoStack.size >= MAX_HISTORY) {
            undoStack.pollFirst()
        }

        undoStack.addLast(VoiceInputItem(text = trimmed))
        redoStack.clear()

        _currentText.value = trimmed
        _lastUndoneText.value = null
        _totalRecordedCount.value = undoStack.size
        updateFlags()
    }

    @Synchronized
    fun undo(context: Context): UndoResult {
        if (undoStack.isEmpty()) {
            return UndoResult(
                success = false,
                restoredText = null,
                undoneText = null,
                userMessage = "আনডু করার মতো কোনো ইনপুট নেই"
            )
        }

        val popped = undoStack.removeLast()
        redoStack.addLast(popped)

        val previousItem = undoStack.peekLast()
        val textToCopy = previousItem?.text ?: ""

        // Copy previous text (or clear) into system clipboard
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Voice Transcription", textToCopy)
            clipboard?.setPrimaryClip(clip)
        } catch (_: Exception) {}

        _currentText.value = if (textToCopy.isNotEmpty()) textToCopy else null
        _lastUndoneText.value = popped.text
        _totalRecordedCount.value = undoStack.size
        updateFlags()

        val msg = if (textToCopy.isNotEmpty()) {
            "আনডু হয়েছে! পূর্বের লেখা ক্লিপবোর্ডে কপি করা হয়েছে"
        } else {
            "আনডু হয়েছে! ভয়েস ইনপুট প্রত্যাহার করা হয়েছে"
        }

        return UndoResult(
            success = true,
            restoredText = if (textToCopy.isNotEmpty()) textToCopy else null,
            undoneText = popped.text,
            userMessage = msg
        )
    }

    @Synchronized
    fun redo(context: Context): RedoResult {
        if (redoStack.isEmpty()) {
            return RedoResult(
                success = false,
                restoredText = null,
                userMessage = "রি-ডু করার মতো কোনো ইনপুট নেই"
            )
        }

        val itemToRestore = redoStack.removeLast()
        undoStack.addLast(itemToRestore)

        // Copy restored text back to clipboard
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Voice Transcription", itemToRestore.text)
            clipboard?.setPrimaryClip(clip)
        } catch (_: Exception) {}

        _currentText.value = itemToRestore.text
        _lastUndoneText.value = null
        _totalRecordedCount.value = undoStack.size
        updateFlags()

        return RedoResult(
            success = true,
            restoredText = itemToRestore.text,
            userMessage = "রি-ডু সম্পন্ন! টেক্সট পুনরুদ্ধার করা হয়েছে"
        )
    }

    @Synchronized
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        _currentText.value = null
        _lastUndoneText.value = null
        _totalRecordedCount.value = 0
        updateFlags()
    }

    private fun updateFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }
}
