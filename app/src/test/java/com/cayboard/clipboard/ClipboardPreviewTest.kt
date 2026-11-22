package com.cayboard.clipboard

import org.junit.Assert.*
import org.junit.Test

class ClipboardPreviewTest {
    @Test fun previewsRemoveLeadingEmptySpaceAndCollapseRepeatedBlankLines() {
        assertEquals("会员健身服务协议\n\n请保管",clipboardRecordPreview("\n\n会员健身服务协议\n\n\n请保管\n"))
        assertEquals("空白文本",clipboardRecordPreview(" \t\n"))
    }
    @Test fun longPreviewIsBoundedAndDoesNotCutSurrogatePairs() {
        val original = "甲".repeat(1199)+"😀"+"乙".repeat(10000)
        val preview = clipboardRecordPreview(original)
        assertEquals("甲".repeat(1199)+"…",preview)
        assertEquals(11201, original.length)
    }
}
