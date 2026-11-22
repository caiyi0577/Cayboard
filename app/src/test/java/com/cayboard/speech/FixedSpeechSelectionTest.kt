package com.cayboard.speech

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cayboard.settings.InputLanguage
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk=[34],application=Application::class)
class FixedSpeechSelectionTest {
    @Test fun savedModelsAndOnlinePreferenceCannotChangeEitherLanguage() {
        val context=ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("asr_model",0).edit().putString("selected_model_zh",SpeechModelCatalog.SENSEVOICE)
            .putString("selected_model_en",SpeechModelCatalog.PARAFORMER).commit()
        context.getSharedPreferences("kime_settings",0).edit().putBoolean("stt_use_local",false)
            .putBoolean("stt_whole_sentence",false).commit()
        val manager=AsrModelManager(context)
        for(language in listOf(InputLanguage.CHINESE,InputLanguage.ENGLISH)) {
            assertEquals(SpeechModelCatalog.XASR,manager.getSelectedModelId(language))
            val selected=manager.selection(SpeechModelCatalog.SENSEVOICE,language)
            assertEquals(SpeechModelCatalog.XASR,selected.mode)
            assertNull(selected.firstDir); assertNull(selected.secondDir)
            assertEquals(SpeechModelCatalog.XASR,selected.finalizerDir?.name)
            assertEquals(if(language==InputLanguage.ENGLISH) "en" else "zh",selected.recognitionLanguage)
            assertFalse(selected.ready)
        }
    }
    @Test fun allFourXasrFilesAreRequiredWithoutSenseVoice() {
        val context=ApplicationProvider.getApplicationContext<Application>()
        val selection=AsrModelManager(context).selection(language=InputLanguage.ENGLISH)
        val dir=selection.finalizerDir!!.apply {mkdirs()}
        val files=listOf("encoder-epoch-99-avg-1.int8.onnx","decoder-epoch-99-avg-1.onnx","joiner-epoch-99-avg-1.int8.onnx","tokens.txt")
        files.forEach { File(dir,it).writeBytes(byteArrayOf(1)) }
        assertTrue(selection.ready)
        File(dir,files.last()).delete()
        assertFalse(selection.ready)
    }
}
