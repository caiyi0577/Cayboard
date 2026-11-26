package com.cayboard.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.system.measureNanoTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ClipboardWordSegmenterTest {
    @Test fun realBundledDictionaryResolvesBoundariesWithoutStartingInputEngine() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(listOf("今天天气", "不错"), ClipboardWordSegmenter.split(context,"今天天气不错"))
        assertEquals(listOf("研究", "生命", "起源"), ClipboardWordSegmenter.split(context,"研究生命起源"))
        assertEquals(listOf("杭研", "大厦"), ClipboardWordSegmenter.split(context,"杭研大厦"))
        assertEquals(listOf("南京市", "长江大桥"), ClipboardWordSegmenter.split(context,"南京市长江大桥"))
    }
    @Test fun originalWhitespaceEmojiAndSupplementaryHanSurviveExactly() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (text in listOf("", "123932625293157", "你好 world 123\r\n😀𠀀！", "👨‍👩‍👧‍👦 🇨🇳 👍🏽\t会员健身服务协议", "a@b.com https://example.org/a?q=123", "\n\n今天  天气不错\n\n")) {
            assertEquals(text, ClipboardWordSegmenter.split(context,text).joinToString(""))
            assertEquals(text, ClipboardWordSegmenter.characters(text).joinToString(""))
        }
        assertEquals(listOf("👨‍👩‍👧‍👦", "🇨🇳", "👍🏽", "𠀀"), ClipboardWordSegmenter.characters("👨‍👩‍👧‍👦🇨🇳👍🏽𠀀"))
    }
    @Test fun oneSharedModelServesConcurrentRequestsAndWarmSplitsAreBounded() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val paragraph = "会员健身服务协议。今天去南京市长江大桥，研究生命起源。请妥善保管本协议。\n"
        val cold = measureNanoTime { ClipboardWordSegmenter.split(context,paragraph) }
        val loads = ClipboardWordSegmenter.loadCount
        val results = (1..40).map { async(Dispatchers.Default) { ClipboardWordSegmenter.split(context,paragraph) } }.awaitAll()
        assertTrue(results.all { it.joinToString("") == paragraph })
        assertEquals(loads, ClipboardWordSegmenter.loadCount)
        val warm = measureNanoTime { repeat(100) { ClipboardWordSegmenter.split(context,paragraph) } }
        val longText = paragraph.repeat(100)
        val longNs = measureNanoTime { assertEquals(longText,ClipboardWordSegmenter.split(context,longText).joinToString("")) }
        assertTrue("Warm requests unexpectedly stalled: ${warm/1e6}ms",warm < 2_000_000_000L)
        assertTrue("Long text unexpectedly stalled: ${longNs/1e6}ms",longNs < 2_000_000_000L)
        System.getenv("CAYBOARD_RENDER_OUT")?.let { out ->
            File(out).mkdirs()
            File(out,"clipboard-split-timing.json").writeText("""{"environment":"Robolectric/JVM; not handset latency","firstRequestMs":${cold/1e6},"warm100RequestsMs":${warm/1e6},"longTextCharacters":${longText.length},"longRequestMs":${longNs/1e6},"modelLoads":$loads}""")
        }
        Unit
    }
}
