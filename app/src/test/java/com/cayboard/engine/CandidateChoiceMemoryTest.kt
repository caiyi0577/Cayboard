package com.cayboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CandidateChoiceMemoryTest {
    private val nativeFirst = EngineCandidate("饿", "e")
    private val second = EngineCandidate("额", "e")
    private val chosen = EngineCandidate("的", "de")
    private val other = EngineCandidate("鹅", "e")

    @Test fun lastChoiceOwnsFirstSlotAndDictionaryOrdersTheTail() {
        val ordered = orderPersonalizedCandidates(
            arrayOf(nativeFirst, second, chosen, other),
            chosen,
            listOf("鹅", "饿", "的"),
        )
        assertEquals(listOf(chosen, other, nativeFirst, second), ordered.toList())
    }

    @Test fun defaultFirstStaysFirstUntilAChoiceHasBeenMade() {
        val ordered = orderPersonalizedCandidates(
            arrayOf(nativeFirst, second, chosen),
            null,
            listOf("的", "额"),
        )
        assertEquals(listOf(nativeFirst, chosen, second), ordered.toList())
    }

    @Test fun rememberedChoiceCanLeadFromOutsideTheDecoderPage() {
        val ordered = orderPersonalizedCandidates(
            arrayOf(nativeFirst, second),
            chosen,
            listOf("额"),
        )
        assertEquals(listOf(chosen, second, nativeFirst), ordered.toList())
    }

    @Test fun unlearnedInputReturnsTheNativeArrayDirectly() {
        val native = arrayOf(nativeFirst, other, second)
        assertSame(native, orderPersonalizedCandidates(native, null, emptyList()))
    }
}
