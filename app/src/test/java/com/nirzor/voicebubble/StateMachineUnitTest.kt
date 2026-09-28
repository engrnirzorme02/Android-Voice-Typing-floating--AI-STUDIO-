package com.nirzor.voicebubble

import com.nirzor.voicebubble.speech.BubbleState
import com.nirzor.voicebubble.speech.BubbleStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StateMachineUnitTest {

    private lateinit var stateMachine: BubbleStateMachine

    @Before
    fun setUp() {
        stateMachine = BubbleStateMachine()
    }

    @Test
    fun testInitialStateIsIdle() {
        assertEquals(BubbleState.Idle, stateMachine.state.value)
        assertTrue(stateMachine.canAcceptTap())
    }

    @Test
    fun testStartListeningFromIdle() {
        val started = stateMachine.startListening()
        assertTrue(started)
        assertTrue(stateMachine.state.value is BubbleState.Listening)
        assertFalse(stateMachine.canAcceptTap())

        // Secondary taps must be ignored
        val tapAgain = stateMachine.startListening()
        assertFalse(tapAgain)
    }

    @Test
    fun testListeningRmsAndPartialUpdates() {
        stateMachine.startListening()
        stateMachine.updateRms(0.75f)
        stateMachine.updatePartialText("আমার সোনার বাংলা")

        val current = stateMachine.state.value as BubbleState.Listening
        assertEquals(0.75f, current.rmsDb)
        assertEquals("আমার সোনার বাংলা", current.partialText)
    }

    @Test
    fun testListeningToProcessingTransition() {
        stateMachine.startListening()
        val transitioned = stateMachine.moveToProcessing()
        assertTrue(transitioned)
        assertEquals(BubbleState.Processing, stateMachine.state.value)
        assertFalse(stateMachine.canAcceptTap())
    }

    @Test
    fun testProcessingToDoneTransition() {
        stateMachine.startListening()
        stateMachine.moveToProcessing()
        val transitioned = stateMachine.moveToDone("কাঁচা টেক্সট", "পরিমার্জিত টেক্সট", true)
        assertTrue(transitioned)

        val doneState = stateMachine.state.value as BubbleState.Done
        assertEquals("কাঁচা টেক্সট", doneState.rawText)
        assertEquals("পরিমার্জিত টেক্সট", doneState.polishedText)
        assertTrue(doneState.copied)
    }

    @Test
    fun testErrorTransitionAndReset() {
        stateMachine.startListening()
        stateMachine.moveToError("মাইক্রোফোন পারমিশন নেই", 9)

        val errorState = stateMachine.state.value as BubbleState.Error
        assertEquals("মাইক্রোফোন পারমিশন নেই", errorState.message)
        assertEquals(9, errorState.errorCode)

        stateMachine.resetToIdle()
        assertEquals(BubbleState.Idle, stateMachine.state.value)
        assertTrue(stateMachine.canAcceptTap())
    }
}
