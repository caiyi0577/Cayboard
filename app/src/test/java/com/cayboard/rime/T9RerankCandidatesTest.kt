package com.cayboard.rime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class T9RerankCandidatesTest {
    private val first = RimeCandidate("你们", "ni men")
    private val partial = RimeCandidate("你", "ni")
    private val alternative = RimeCandidate("你嫩", "ni'nen")
    private val correction = RimeCandidate("拟闷", "ni men", correction = "mi=ni")
    private val result = RimeProcessResult(true, "", "64636", "ni men",
        arrayOf(first, partial, alternative, correction), false, false, false, engineRevision = 42)

    @Test fun onlyWholeExactCandidatesCanMove() {
        val indexes = T9RerankCandidates.eligible(result)
        assertEquals(listOf(0, 2), indexes)
        val ranked = T9RerankCandidates.promote(result, indexes, floatArrayOf(-2f, -1f))
        assertEquals(listOf(alternative, first, partial, correction), ranked.candidates.toList())
        assertEquals(result.engineRevision, ranked.engineRevision)
        assertEquals(result.preeditText, ranked.preeditText)
    }

    @Test fun fixedLettersPartialFirstAndLaterPagesKeepNativeOrder() {
        assertEquals(emptyList<Int>(), T9RerankCandidates.eligible(result.copy(inputText = "646m6")))
        assertEquals(emptyList<Int>(), T9RerankCandidates.eligible(result.copy(hasPrevPage = true)))
        assertEquals(emptyList<Int>(), T9RerankCandidates.eligible(result.copy(candidates = arrayOf(partial, first, alternative))))
        assertSame(result, T9RerankCandidates.promote(result, listOf(0, 2), floatArrayOf(-1f, -0.95f)))
        assertSame(result, T9RerankCandidates.promote(result, listOf(0, 2), floatArrayOf(-1f, Float.NaN)))
    }
}
