package com.cayboard.association

import android.content.Context
import android.util.Log
import com.cayboard.settings.SettingsPreferences
import com.cayboard.util.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AssociationManager {
    private const val TAG = "AssociationManager"
    
    @Volatile
    private var bundledModel: BundledAssociationModel? = null
    private val mutex = Mutex()
    private val generation = java.util.concurrent.atomic.AtomicLong()
    
    suspend fun initialize(ctx: Context): Boolean {
        val token = generation.get()
        if (bundledModel != null) return true
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                if (token != generation.get()) return@withLock false
                if (bundledModel != null) return@withLock true
                if (!SettingsPreferences.isSmartPredictionEnabled(ctx)) return@withLock false
                try {
                    val engine = BundledAssociationModel(ctx.applicationContext)
                    if (!engine.initialize()) return@withLock false
                    synchronized(this@AssociationManager) {
                        if (token != generation.get()) return@withLock false
                        bundledModel = engine
                    }
                    true
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (e: Exception) {
                    FileLogger.e(TAG, "Failed to initialize AssociationManager: ${e.message}", e)
                    false
                }
            }
        }
    }
    
    /** [afterWholeWord]: the last commit was a phrase of two or more characters; see [BaseAssociationModel.predict]. */
    suspend fun predict(contextText: String, topK: Int = 20, afterWholeWord: Boolean = false): List<AssociationCandidate> = withContext(Dispatchers.Default) {
        val engine = bundledModel ?: mutex.withLock { bundledModel } ?: return@withContext emptyList()
        
        try {
            usableCandidates(engine.predict(contextText, afterWholeWord)).take(topK)
            
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (e: Exception) {
            Log.e(TAG, "Prediction failed", e)
            emptyList()
        }
    }
    
    /**
     * 去掉不能当联想词的候选：模型在英文字母后会“续写”单个字母（e → n s r i…）；学过的句尾标点会被当成
     * 下一个“词”预测出来（打完“吗”总冒出 ？" ），只有标点和空白的候选一律不出（表情照常）。
     */
    internal fun usableCandidates(candidates: List<AssociationCandidate>): List<AssociationCandidate> =
        candidates.filterNot { candidate ->
            val text = candidate.text
            (text.length == 1 && text[0].let { c -> c in 'a'..'z' || c in 'A'..'Z' }) ||
                text.codePoints().allMatch { Character.isWhitespace(it) || isPunctuation(it) }
        }

    private fun isPunctuation(codePoint: Int): Boolean = when (Character.getType(codePoint).toByte()) {
        Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION,
        Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
        Character.OTHER_PUNCTUATION -> true
        else -> false
    }

    fun isInitialized(): Boolean = bundledModel != null
    
    @Synchronized fun release() {
        generation.incrementAndGet()
        bundledModel = null
    }

}
