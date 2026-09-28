package com.nirzor.voicebubble

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nirzor.voicebubble.data.BubbleThemes
import com.nirzor.voicebubble.data.UndoRedoManager
import com.nirzor.voicebubble.updater.UpdateManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoiceBubbleUnitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        UndoRedoManager.clear()
    }

    @Test
    fun testBubbleThemesRetrieval() {
        val defaultTheme = BubbleThemes.getThemeById("indigo_ocean")
        assertNotNull(defaultTheme)
        assertEquals("indigo_ocean", defaultTheme.id)

        val emeraldTheme = BubbleThemes.getThemeById("emerald_mint")
        assertEquals("emerald_mint", emeraldTheme.id)

        val fallbackTheme = BubbleThemes.getThemeById("unknown_theme_id")
        assertEquals("indigo_ocean", fallbackTheme.id)
    }

    @Test
    fun testUndoRedoManagerFlow() {
        assertEquals(false, UndoRedoManager.canUndo.value)
        assertEquals(false, UndoRedoManager.canRedo.value)

        // 1. Record first voice input
        UndoRedoManager.recordInput(context, "হ্যালো বাংলাদেশ")
        assertEquals(true, UndoRedoManager.canUndo.value)
        assertEquals(false, UndoRedoManager.canRedo.value)
        assertEquals("হ্যালো বাংলাদেশ", UndoRedoManager.currentText.value)

        // 2. Record second voice input
        UndoRedoManager.recordInput(context, "আমি বাংলায় কথা বলি")
        assertEquals(true, UndoRedoManager.canUndo.value)
        assertEquals("আমি বাংলায় কথা বলি", UndoRedoManager.currentText.value)

        // 3. Test Undo
        val undoResult = UndoRedoManager.undo(context)
        assertTrue(undoResult.success)
        assertEquals("হ্যালো বাংলাদেশ", undoResult.restoredText)
        assertEquals("আমি বাংলায় কথা বলি", undoResult.undoneText)
        assertEquals(true, UndoRedoManager.canUndo.value)
        assertEquals(true, UndoRedoManager.canRedo.value)
        assertEquals("হ্যালো বাংলাদেশ", UndoRedoManager.currentText.value)

        // 4. Test Redo
        val redoResult = UndoRedoManager.redo(context)
        assertTrue(redoResult.success)
        assertEquals("আমি বাংলায় কথা বলি", redoResult.restoredText)
        assertEquals(true, UndoRedoManager.canUndo.value)
        assertEquals(false, UndoRedoManager.canRedo.value)
        assertEquals("আমি বাংলায় কথা বলি", UndoRedoManager.currentText.value)

        // 5. Test Undo all the way to empty
        UndoRedoManager.undo(context)
        val undoToEmpty = UndoRedoManager.undo(context)
        assertTrue(undoToEmpty.success)
        assertNull(undoToEmpty.restoredText)
        assertEquals(false, UndoRedoManager.canUndo.value)
        assertEquals(true, UndoRedoManager.canRedo.value)

        // 6. Test Undo when empty
        val emptyUndo = UndoRedoManager.undo(context)
        assertFalse(emptyUndo.success)
    }

    @Test
    fun testVersionCodeCalculation() {
        assertEquals(10000, UpdateManager.parseVersionCode("1.0.0"))
        assertEquals(10100, UpdateManager.parseVersionCode("v1.1.0"))
        assertEquals(10203, UpdateManager.parseVersionCode("1.2.3"))
    }
}
