package com.nirzor.voicebubble.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Stack

data class UndoRedoResult(
    val success: Boolean,
    val restoredText: String?,
    val undoneText: String? = null,
    val userMessage: String
)

object UndoRedoManager {

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _currentText = MutableStateFlow<String?>(null)
    val currentText: StateFlow<String?> = _currentText.asStateFlow()

    private val _totalRecordedCount = MutableStateFlow(0)
    val totalRecordedCount: StateFlow<Int> = _totalRecordedCount.asStateFlow()

    fun recordInput(context: Context, newText: String) {
        if (newText.isBlank()) return
        if (undoStack.isNotEmpty() && undoStack.peek() == newText) return

        undoStack.push(newText)
        redoStack.clear()
        _canUndo.value = undoStack.size > 1 || undoStack.isNotEmpty()
        _canRedo.value = false
        _currentText.value = newText
        _totalRecordedCount.value = undoStack.size
    }

    fun undo(context: Context): UndoRedoResult {
        if (undoStack.isEmpty()) {
            return UndoRedoResult(
                success = false,
                restoredText = null,
                userMessage = "পূর্বাবস্থায় ফেরার মতো কিছু নেই"
            )
        }

        val current = undoStack.pop()
        redoStack.push(current)

        val previous = if (undoStack.isNotEmpty()) undoStack.peek() else null
        _currentText.value = previous
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true

        if (previous != null) {
            copyToClipboard(context, previous)
        }

        return UndoRedoResult(
            success = true,
            restoredText = previous,
            undoneText = current,
            userMessage = if (previous != null) "আনডু সফল হয়েছে: আগের লেখা কপি হলো" else "আনডু সফল: বাফার খালি করা হয়েছে"
        )
    }

    fun redo(context: Context): UndoRedoResult {
        if (redoStack.isEmpty()) {
            return UndoRedoResult(
                success = false,
                restoredText = null,
                userMessage = "পুনরুদ্ধার করার মতো কিছু নেই"
            )
        }

        val textToRestore = redoStack.pop()
        undoStack.push(textToRestore)

        _currentText.value = textToRestore
        _canUndo.value = true
        _canRedo.value = redoStack.isNotEmpty()

        copyToClipboard(context, textToRestore)

        return UndoRedoResult(
            success = true,
            restoredText = textToRestore,
            userMessage = "রি-ডু সফল হয়েছে: লেখাটি ক্লিপবোর্ডে কপি হলো"
        )
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Voice Transcription", text)
        clipboard.setPrimaryClip(clip)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _currentText.value = null
        _totalRecordedCount.value = 0
    }
}
