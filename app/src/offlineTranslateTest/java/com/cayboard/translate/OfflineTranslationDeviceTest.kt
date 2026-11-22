package com.cayboard.translate

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

@RunWith(AndroidJUnit4::class)
class OfflineTranslationDeviceTest {
    @Test fun builtinEntryRunsThePackagedOfflineToolWithoutAnOnlineConfiguration() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val active = TranslationController(app)
        val store = app.getSharedPreferences("translation", android.content.Context.MODE_PRIVATE)
        val previous = listOf("sourceLang", "targetLang").associateWith { store.getString(it, null) }
        assertEquals(android.content.pm.PackageManager.PERMISSION_DENIED,
            app.checkSelfPermission(android.Manifest.permission.INTERNET))
        try {
        active.onPanelAction("select:sourceLang:简体中文")
        active.onPanelAction("select:targetLang:英文")
        active.onPanelInput("我会在十分钟后到达。")
        active.onPanelAction("generate")
        val finish=System.currentTimeMillis()+30000
        var state=active.getPanelState("")
        while(state.loading && System.currentTimeMillis()<finish) {Thread.sleep(50);state=active.getPanelState("")}
        assertFalse(state.loading);assertTrue(state.items.isNotEmpty())
        assertTrue(state.items.first().text.any {it in 'a'..'z' || it in 'A'..'Z'})
        } finally {
            previous.forEach {(key,value)->if(value==null) store.edit().remove(key).commit() else store.edit().putString(key,value).commit()}
            active.onPanelInput("");active.onPanelAction("generate")
        }
    }
    private fun ready():android.content.Context {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        BundledTranslationModel.prepare(context)
        assertEquals(BundledTranslationModel.failure,"ready",BundledTranslationModel.phase.value)
        return context
    }
    @Test fun bundledModelTranslatesMultipleLanguagesAndRetiresOnlyTheOldPair() {
        val context=ready();val results=JSONArray()
        assertEquals(BundledTranslationModel.SIZE,BundledTranslationModel.file(context).length())
        assertFalse(File(context.filesDir,"models/translate-zh-en").exists())
        assertFalse(File(context.filesDir,"models/translate-en-zh").exists())
        for ((from,to,text) in listOf(
            Triple("zh","en","你好，明天见。"),Triple("en","zh","I will arrive in ten minutes."),
            Triple("zh","ja","请问附近的地铁站在哪里？"),Triple("ja","zh","この店は何時に閉まりますか？"),
            Triple("zh","ko","我想预订明天晚上的房间。"),Triple("fr","en","Combien coûte ce billet ?"),
            Triple("en","fr","Please send me the address."),Triple("zh","ru","请问这个多少钱？"),
            Triple("en","es","I will call you tomorrow."),Triple("en","ar","Where is the nearest hospital?"),
            Triple("auto","zh","Merci pour votre aide."),Triple("auto","en","また明日会いましょう。")
        )) {
            val latch=CountDownLatch(1);var output="";var error="";val start=System.nanoTime()
            OfflineTranslation.translate(context,text,from,to,object:OfflineTranslateListener {
                override fun onDone(value:String) {output=value;latch.countDown()}
                override fun onError(message:String) {error=message;latch.countDown()}
            })
            assertTrue("Translation timed out for $from-$to",latch.await(45,TimeUnit.SECONDS))
            assertEquals("",error);assertTrue(output.isNotBlank());assertNotEquals(text,output)
            when(to) {
                "zh" -> assertTrue(output.any {it in '\u4e00'..'\u9fff'})
                "ja" -> assertTrue(output.any {it in '\u3040'..'\u30ff'})
                "ko" -> assertTrue(output.any {it in '\uac00'..'\ud7af'})
                "ru" -> assertTrue(output.any {it in '\u0400'..'\u04ff'})
                "ar" -> assertTrue(output.any {it in '\u0600'..'\u06ff'})
                else -> assertTrue(output.any {it in 'a'..'z' || it in 'A'..'Z'})
            }
            results.put(JSONObject().put("from",from).put("to",to).put("source",text).put("result",output).put("milliseconds",(System.nanoTime()-start)/1000000))
            File(context.filesDir,"hymt-multilingual-smoke.json").writeText(results.toString(2))
        }
    }
    @Test fun aNewSentenceCancelsTheOldInferenceAndNeverDeliversItsStaleResult() {
        val context=ready();val oldDelivered=java.util.concurrent.atomic.AtomicBoolean(false)
        val oldId=OfflineTranslation.translate(context,"请将这段话翻译为英语，不要遗漏细节。".repeat(30),"zh","en",object:OfflineTranslateListener {
            override fun onDone(text:String) {oldDelivered.set(true)}
            override fun onError(message:String) {oldDelivered.set(true)}
        })
        Thread.sleep(150);OfflineTranslation.cancel(oldId)
        val done=CountDownLatch(1);var result="";var error=""
        OfflineTranslation.translate(context,"你好，明天见。","zh","ja",object:OfflineTranslateListener {
            override fun onDone(text:String) {result=text;done.countDown()}
            override fun onError(message:String) {error=message;done.countDown()}
        })
        assertTrue(done.await(45,TimeUnit.SECONDS));assertEquals("",error);assertTrue(result.isNotBlank());assertFalse(oldDelivered.get())
    }
}
