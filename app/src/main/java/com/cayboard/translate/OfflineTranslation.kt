package com.cayboard.translate

import android.content.Context
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** One bundled multilingual model. All disk preparation and inference stay off the IME thread. */
object OfflineTranslation {
    private val worker = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "offline-translate").apply { isDaemon = true } }
    private var engine = 0L
    private var idleClose: ScheduledFuture<*>? = null
    private val nextId = AtomicInteger()
    @Volatile private var newestId = -1
    @Volatile private var cancelledId = -1
    private fun closeEngine() { if (engine != 0L) OfflineTranslatorNative.nativeDestroy(engine); engine = 0L }
    fun translate(context: Context, text: String, from: String, to: String, listener: OfflineTranslateListener): Int {
        if (!TranslationLanguages.supports(from,to) || !OfflineTranslatorNative.load()) {
            listener.onError("不支持这个翻译方向或设备"); return -1
        }
        if (text.isBlank() || text.codePointCount(0,text.length)>1000) {
            listener.onError("请输入不超过1000字的文本"); return -1
        }
        val id=nextId.getAndIncrement();newestId=id
        OfflineTranslatorNative.nativeCancel()
        val app=context.applicationContext
        worker.execute {
            if(id!=newestId || id==cancelledId) return@execute
            idleClose?.cancel(false)
            val result=runCatching {
                val model=BundledTranslationModel.prepare(app)
                if(engine==0L) engine=OfflineTranslatorNative.nativeCreate(model.absolutePath,4)
                if(id!=newestId || id==cancelledId) return@runCatching ""
                if(from.equals(to,ignoreCase=true)) text else OfflineTranslatorNative.nativeTranslate(engine,text,
                    TranslationLanguages.byCode(from)?.promptName ?: "auto",TranslationLanguages.byCode(to)!!.promptName)
            }
            idleClose=worker.schedule({closeEngine()},60,TimeUnit.SECONDS)
            if(id!=newestId || id==cancelledId) return@execute
            result.onSuccess { if(it.isBlank()) listener.onError("翻译没有返回结果") else listener.onDone(it) }.onFailure {
                closeEngine();listener.onError("离线翻译失败：${it.message ?: "请重试"}")
            }
        }
        return id
    }
    fun cancel(id:Int) {if(id>=0) {cancelledId=id;if(id==newestId && OfflineTranslatorNative.load()) OfflineTranslatorNative.nativeCancel()}}
}
