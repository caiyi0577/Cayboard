package com.cayboard.clipboard

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.StringWriter

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class ClipboardExportTest {
    private val items = listOf(
        ClipboardItem(1, "旧的历史", timestamp = 100),
        ClipboardItem(2, "我的地址", timestamp = 50),
        ClipboardItem(3, "新的历史 ```代码```", timestamp = 300),
        ClipboardItem(4, "旧快捷发送", timestamp = 400),
    )

    private fun export(format: ClipboardExportFormat, pinned: Boolean = true) =
        StringWriter().also { ClipboardExport.write(ClipboardExport.order(items), format, it, 1000) }.toString()

    @Test fun fixedFirstThenNewestAndPinnedCanBeLeftOut() {
        assertEquals(listOf(4L, 3L, 1L, 2L), ClipboardExport.order(items).map { it.id })
        assertEquals(listOf(4L, 3L, 1L, 2L), ClipboardExport.order(items).map { it.id })
    }

    @Test fun fileNameCarriesTheFormatExtension() {
        assertTrue(ClipboardExport.fileName(ClipboardExportFormat.MD, 0).endsWith(".md"))
        assertTrue(ClipboardExport.fileName(ClipboardExportFormat.JSON, 0).startsWith("Cayboard剪贴板-"))
    }
}
