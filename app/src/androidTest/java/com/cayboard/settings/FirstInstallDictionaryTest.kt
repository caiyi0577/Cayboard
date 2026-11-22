package com.cayboard.settings

import androidx.test.platform.app.InstrumentationRegistry
import com.cayboard.rime.RimeConfigHelper
import com.cayboard.rime.RimeEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FirstInstallDictionaryTest {
    @Test fun bundledWordsWorkWithoutUserDictionaryOrPrediction() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val (user, shared) = RimeConfigHelper.initializeRimeDataAsync(context)
        val engine = RimeEngine.getInstance()
        engine.initialize(user, shared)
        assertTrue(RimeConfigHelper.ensureDeployment(context))
        assertTrue(engine.ensureSession())
        assertTrue(SettingsPreferences.shouldShowCandidateCancelButton(context))
        assertFalse(SettingsPreferences.isSmartPredictionEnabled(context))
        val samples = mapOf(
            "rime_ice" to listOf("zenme" to "怎么", "zenm" to "怎么", "dazi" to "打字"),
            "t9_pinyin" to listOf("93663" to "怎么", "3294" to "打字"),
            "pinyin_14jian" to listOf("zebme" to "怎么"),
            "double_pinyin_flypy" to listOf("zfme" to "怎么"),
            "double_pinyin_flypy_14jian" to listOf("zdme" to "怎么", "bugc" to "你好", "ceuu" to "测试", "zfme" to "怎么"))
        for ((schema, cases) in samples) {
            assertTrue(engine.switchSchema(schema))
            engine.setOption("ascii_mode", false)
            for ((code, word) in cases) {
                engine.clearQueuedComposition()
                engine.setInput(code)
                val candidates = engine.getCandidates().toList()
                assertTrue("$schema $code -> $candidates; missing $word", candidates.any { it == word })
            }
        }
        engine.clearQueuedComposition()
        val table = java.io.File(context.filesDir, "rime/build/rime_ice.table.bin")
        val backup = java.io.File(table.parentFile, "rime_ice.table.bin.test-backup")
        assertTrue(table.renameTo(backup))
        try { assertFalse("schema YAML alone must not mark deployment complete", RimeConfigHelper.isDeploymentComplete(context)) }
        finally { assertTrue(backup.renameTo(table)) }
    }
}
