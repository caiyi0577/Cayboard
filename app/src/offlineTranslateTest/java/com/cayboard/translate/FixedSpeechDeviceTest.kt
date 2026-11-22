package com.cayboard.translate

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.settings.InputLanguage
import com.cayboard.speech.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class FixedSpeechDeviceTest {
    @Test fun bilingualRecognitionRunsWithoutAnyOtherSpeechModel() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        FixedSpeechModel.prepare(context)
        for(id in listOf(SpeechModelCatalog.SENSEVOICE,SpeechModelCatalog.PARAFORMER)) {
            assertFalse(File(context.filesDir,"models/$id").exists())
        }
        val output=JSONArray()
        for(language in listOf(InputLanguage.CHINESE,InputLanguage.ENGLISH)) {
            val selected=AsrModelManager(context).selectionForLanguage(language)
            assertEquals(SpeechModelCatalog.XASR,selected.mode)
            assertNull(selected.secondDir)
            SherpaSpeechEngine(context,selected).use {engine ->
                for(index in 0..4) {
                    val bytes=instrumentation.context.assets.open("xasr-smoke/$index.pcm").use {it.readBytes()}
                    val short=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val pcm=FloatArray(short.remaining()) {short.get()/32768f}
                    val start=android.os.SystemClock.elapsedRealtime()
                    val text=engine.refinePunctuated(pcm)
                    assertTrue("sample $index / ${language.id}",text.isNotBlank())
                    if (index == 4) {
                        assertTrue(text, Regex("[A-Za-z]{4,}").findAll(text).count() >= 8)
                        assertFalse(text, Regex("[\\p{IsHan}]").containsMatchIn(text))
                    }
                    output.put(JSONObject().put("language",language.id).put("sample",index).put("text",text)
                        .put("milliseconds",android.os.SystemClock.elapsedRealtime()-start))
                }
            }
        }
        val words=(0 until output.length()).map {output.getJSONObject(it).getString("text")}
        assertTrue(words.any {Regex("[a-zA-Z]{3,}").containsMatchIn(it)})
        assertTrue(words.any {Regex("[\\p{IsHan}]").containsMatchIn(it)})
        File(context.filesDir,"fixed-xasr-smoke.json").writeText(output.toString(2))
    }
}
