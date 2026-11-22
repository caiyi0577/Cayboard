package com.cayboard.service
import org.junit.Assert.*
import org.junit.Test
class OcrDraftTransitionsTest {
    @Test fun openingReviewTargetsOnlyOcrTextAndEnterRemainsNewline() {
        val opened=OcrDraftTransitions.open(InputUIState(enterKeyText="发送"),"识别结果")
        assertTrue(opened.showOcrDraft)
        assertEquals("识别结果",opened.ocrDraftText)
        assertEquals(InputTarget.OcrDraft,opened.inputTarget)
        assertEquals("换行",opened.enterKeyText)
    }
    @Test fun closingReviewClearsItsTextFocusAndRestoresTheHostKey() {
        val closed=OcrDraftTransitions.close(OcrDraftTransitions.open(InputUIState(),"识别结果"),"发送")
        assertFalse(closed.showOcrDraft)
        assertFalse(closed.ocrDraftFocused)
        assertEquals("",closed.ocrDraftText)
        assertEquals("发送",closed.enterKeyText)
        assertEquals(InputTarget.Host,closed.inputTarget)
    }
}
