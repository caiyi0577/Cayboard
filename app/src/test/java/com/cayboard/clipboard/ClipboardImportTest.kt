package com.cayboard.clipboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.StringWriter
@RunWith(AndroidJUnit4::class)
@Config(sdk=[34],application=android.app.Application::class)
class ClipboardImportTest {
    @Test fun jsonAndMarkdownKeepUnicodeTextAndTimestamps() {
        val rows=listOf(ClipboardItem(1,"中文😀\n第二行",timestamp=1700000040000),ClipboardItem(2,"```代码```",timestamp=1700000100000))
        for(format in listOf(ClipboardExportFormat.JSON,ClipboardExportFormat.MD)) {
            val out=StringWriter();ClipboardExport.write(rows,format,out,1700000100000)
            val result=ClipboardImport.parse(out.toString())
            assertEquals(rows.map {it.text},result.map {it.text})
            assertEquals(rows.map {it.timestamp},result.map {it.timestamp})
        }
    }
    @Test fun legacyFlagsAreIgnoredAndTextIsRetained() {
        val records=ClipboardImport.parse("""{"items":[{"text":"旧文字","code":"abc","pinned":true,"timestamp":123}]}""")
        assertEquals(listOf(ImportedClip("旧文字",123)),records)
    }
    @Test fun plainTextAndInvalidJsonRemainLiteral() {
        assertEquals("{不是 JSON",ClipboardImport.parse("{不是 JSON").single().text)
        assertTrue(ClipboardImport.parse("").isEmpty())
    }
}
