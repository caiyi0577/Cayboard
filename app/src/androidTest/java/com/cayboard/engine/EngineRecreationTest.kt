package com.cayboard.engine

import androidx.test.platform.app.InstrumentationRegistry
import com.cayboard.settings.SchemaManager
import com.cayboard.settings.SettingsPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class EngineRecreationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun assertChineseWorks(engine: InputEngine) {
        assertTrue(engine.ensureSession(10_000L))
        val schemas = engine.getAvailableSchemas().toSet()
        assertTrue("Chinese schemas disappeared: $schemas", schemas.containsAll(listOf("cayboard_pinyin", "t9_pinyin")))
        for ((schema, code) in listOf("cayboard_pinyin" to "zenme", "t9_pinyin" to "93663")) {
            assertTrue("Cannot select $schema", engine.switchSchema(schema))
            engine.setOption("ascii_mode", false)
            engine.clearQueuedComposition()
            engine.setInput(code)
            assertTrue("No Chinese candidates for $schema/$code: ${engine.getCandidates().toList()}",
                engine.getCandidates().any { it == "怎么" })
        }
        engine.clearQueuedComposition()
    }

    @Test fun preparationRecreatesDestroyedEngineInsteadOfTrustingProcessCache(): Unit = runBlocking {
        assertTrue(EngineConfig.prepareEngine(context))
        val engine = InputEngine.getInstance()
        assertChineseWorks(engine)
        repeat(3) {
            engine.destroy() // Same native lifecycle as switching away and destroying the IME service.
            assertFalse(InputEngine.isInitialized())
            assertTrue(EngineConfig.prepareEngine(context))
            assertTrue("Preparation reported success with a destroyed engine (round $it)", InputEngine.isInitialized())
            assertChineseWorks(engine)
        }
    }

    @Test fun buildFilesAloneCannotReportReadyAfterEngineDestruction(): Unit = runBlocking {
        assertTrue(EngineConfig.prepareEngine(context))
        InputEngine.getInstance().destroy()
        try {
            assertFalse("A build hash cannot prove a live input engine", EngineConfig.ensureDeployment(context))
            assertFalse(SettingsPreferences.isDeploymentDone(context))
        } finally {
            assertTrue(EngineConfig.prepareEngine(context))
        }
    }

    @Test fun settingsDeploymentRepairsDestroyedEngineAndPublishesRefresh(): Unit = runBlocking {
        assertTrue(EngineConfig.prepareEngine(context))
        val prefs = SettingsPreferences.getPrefsPublic(context)
        val previous = prefs.getLong(EngineConfig.DEPLOYMENT_REVISION, 0L)
        InputEngine.getInstance().destroy()
        assertTrue(EngineConfig.redeploy(context))
        assertTrue(SettingsPreferences.isDeploymentDone(context))
        assertTrue(prefs.getLong(EngineConfig.DEPLOYMENT_REVISION, 0L) > previous)
        assertChineseWorks(InputEngine.getInstance())
    }
}
