package com.cayboard.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.cayboard.speech.AsrMemoryPressure
import com.cayboard.speech.AsrModelManager
import com.cayboard.speech.LocalSpeechSession
import com.cayboard.speech.ResidentSpeechSessionOwner
import com.cayboard.speech.SherpaSpeechEngine
import com.cayboard.util.FileLogger
import com.cayboard.settings.InputLanguage
import java.util.concurrent.atomic.AtomicLong

/** Model weights and both decode workers live in :asr, never on the keyboard UI thread. */
class AsrInferenceService : Service() {
    private val lock = Any()
    private val idleHandler = Handler(Looper.getMainLooper())
    private val sessions = ResidentSpeechSessionOwner()
    private var keepAlive = false
    private var engineIdleMs = 60_000L
    /** 最近一次语音输入用的模型；结束后据此在后台准备 Zipformer 的快速加载副本。 */
    @Volatile private var lastDictation: AsrModelManager.Selection? = null
    private val generation = AtomicLong()
    private val idleRelease = Runnable {
        synchronized(lock) {
            if (!keepAlive) {
                sessions.release(onlyIfIdle = true)

            }
        }
    }
    private fun cancelIdleRelease() {
        idleHandler.removeCallbacks(idleRelease)
    }
    private fun scheduleRelease() {
        synchronized(lock) {
            cancelIdleRelease()
            if (!sessions.isIdle) return
            val engineMs = if (keepAlive) 0L else engineIdleMs
            if (engineMs > 0) idleHandler.postDelayed(idleRelease, engineMs)
        }
    }

    private val binder = object : IInferenceAsrService.Stub() {
        override fun startAsr(modelId: String, callback: IInferenceAsrCallback): Boolean =
            startSession(modelId, null, callback)
        override fun startAsrForLanguage(modelId: String, languageId: String, callback: IInferenceAsrCallback): Boolean {
            val language = InputLanguage.fromId(languageId)
            if (language == null || language == InputLanguage.UNSPECIFIED) {
                callback.onError("当前方案未标注可用的语音语言")
                return false
            }
            return startSession(modelId, language, callback)
        }
        private fun startSession(modelId: String, language: InputLanguage?, callback: IInferenceAsrCallback): Boolean =
            startDictation(modelId, language, callback)
        private fun startDictation(modelId: String, language: InputLanguage?, callback: IInferenceAsrCallback): Boolean {
            cancelIdleRelease()
            val token = generation.incrementAndGet()
            val startedAt = SystemClock.elapsedRealtime()
            var constructed = false
            var modelLoadMs = 0L
            return try {
                com.cayboard.speech.FixedSpeechModel.prepare(this@AsrInferenceService)
                val selection = AsrModelManager(this@AsrInferenceService).selection(modelId, language)
                check(selection.ready) { "内置语音模型准备失败" }
                val selectedAt = SystemClock.elapsedRealtime()
                val firstText = java.util.concurrent.atomic.AtomicBoolean(false)
                val ready = sessions.start(selection.key,
                    createEngine = {
                        constructed = true
                        val loadingAt = SystemClock.elapsedRealtime()
                        try { SherpaSpeechEngine(this@AsrInferenceService, selection) }
                        finally { modelLoadMs = SystemClock.elapsedRealtime() - loadingAt }
                    },
                    createSession = { engine -> LocalSpeechSession(engine,
                        onText = { text ->
                            // Release builds too: `adb logcat -s Cayboard-AsrTiming` measures the delay to the first words.
                            if (text.isNotEmpty() && firstText.compareAndSet(false, true)) android.util.Log.i("Cayboard-AsrTiming",
                                "first text ${SystemClock.elapsedRealtime() - startedAt} ms after start mode=${selection.mode}")
                            if (generation.get() == token) try { callback.onPartialResult(text) } catch (_: Exception) { }
                        },
                        onError = { message ->
                            if (generation.get() == token) try { callback.onError(message) } catch (_: Exception) { }
                        },
                        wholeSentence = true) },
                    owner = callback.asBinder(), isActive = { generation.get() == token })
                val readyAt = SystemClock.elapsedRealtime()
                val modelState = if (constructed) "loaded" else if (ready) "resident" else "superseded"
                android.util.Log.i("Cayboard-AsrTiming", "start ready=$ready model=$modelState modelMs=$modelLoadMs " +
                    "totalMs=${readyAt - startedAt} mode=${selection.mode}")
                FileLogger.i("AsrInferenceService", "start ready=$ready model=$modelState " +
                    "selectionMs=${selectedAt - startedAt} modelMs=$modelLoadMs " +
                    "sessionWaitMs=${readyAt - selectedAt - modelLoadMs} totalMs=${readyAt - startedAt}")
                if (ready) lastDictation = selection else scheduleRelease()
                ready
            } catch (e: Exception) {
                FileLogger.e("AsrInferenceService", "startAsr failed", e)
                if (generation.get() == token) callback.onError(e.message ?: "离线语音模型加载失败")
                scheduleRelease()
                false
            } catch (e: LinkageError) {
                if (generation.get() == token) callback.onError("识别运行库无法加载：${e.message}")
                scheduleRelease()
                false
            }
        }
        override fun pushAsrAudio(session: IBinder, audioData: ByteArray) {
            sessions.acceptPcm(audioData, session)
        }
        override fun stopAsr(session: IBinder): String {
            val token = generation.get()
            FileLogger.d("AsrInferenceService", "stop generation=$token idle=${sessions.isIdle}")
            val text = sessions.finish(session) {
                generation.compareAndSet(token, token + 1)
                scheduleRelease()
                lastDictation?.let { com.cayboard.speech.OptimizedEncoder.prepareLater(this@AsrInferenceService, it) }
            } ?: ""
            return text
        }
        override fun cancelAsr(session: IBinder) {
            FileLogger.d("AsrInferenceService", "cancel generation=${generation.get()} idle=${sessions.isIdle}")
            val token = generation.get()
            if (sessions.cancel(session)) generation.compareAndSet(token, token + 1)
            scheduleRelease()
        }
        override fun releaseAsr(session: IBinder) {
            FileLogger.d("AsrInferenceService", "release generation=${generation.get()} idle=${sessions.isIdle}")
            val token = generation.get()
            if (sessions.release(owner = session)) {
                generation.compareAndSet(token, token + 1)
                cancelIdleRelease()
            }
        }
        override fun onKeyboardHidden() {
            val memory = android.app.ActivityManager.MemoryInfo()
            try { getSystemService(android.app.ActivityManager::class.java)?.getMemoryInfo(memory) ?: return }
            catch (_: Exception) { return }
            if (!AsrMemoryPressure.isTight(memory.availMem, memory.threshold, memory.totalMem, memory.lowMemory)) return
            synchronized(lock) {
                if (!sessions.isIdle) return
                FileLogger.w("AsrInferenceService", "keyboard hidden, memory tight (avail=${memory.availMem shr 20} MB " +
                    "threshold=${memory.threshold shr 20} MB total=${memory.totalMem shr 20} MB): releasing voice models")
                releaseIdleModels()
            }
        }
        override fun releaseIdleModelsNow(): Boolean = synchronized(lock) {
            if (!sessions.isIdle) return false
            FileLogger.i("AsrInferenceService", "release requested from settings: releasing voice models")
            releaseIdleModels()
            true
        }
        override fun memoryKb(): Long = com.cayboard.settings.MemoryStats.selfPssKb() ?: 0L
        override fun setEngineIdleSeconds(engineIdleSeconds: Int) = synchronized(lock) {
            engineIdleMs = engineIdleSeconds.coerceAtLeast(0) * 1000L
            keepAlive = engineIdleMs == 0L
            cancelIdleRelease()
            if (sessions.isIdle) scheduleRelease()
        }
    }
    /**
     * 系统内存紧张或进程进入后台列表时，释放空闲的 X-ASR。
     * 正在说话时保留模型。
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        synchronized(lock) {
            if (!sessions.isIdle) return
            val all = level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
                level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
            if (all) releaseIdleModels()
        }
    }

    /** 调用方持有 [lock]，且已确认语音会话已结束。 */
    private fun releaseIdleModels() {
        cancelIdleRelease()
        sessions.release(onlyIfIdle = true)
    }

    override fun onBind(intent: Intent): IBinder = binder
    override fun onDestroy() {
        generation.incrementAndGet()
        cancelIdleRelease()
        sessions.release()
        super.onDestroy()
    }
}
