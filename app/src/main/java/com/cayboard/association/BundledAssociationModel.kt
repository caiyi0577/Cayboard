package com.cayboard.association

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class BundledAssociationModel(private val context: Context) {
    private var baseModel: BaseAssociationModel? = null

    suspend fun initialize(): Boolean {
        baseModel = withContext(Dispatchers.IO) {
            // Without the marks the table still works; after a whole word it then behaves as before.
            val wordCompletions = try {
                context.assets.open(BaseAssociationModel.WORD_COMPLETIONS_ASSET).use { input ->
                    java.util.zip.GZIPInputStream(input).reader(Charsets.UTF_8).use(BaseAssociationModel::readWordCompletions)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Association word completion marks failed to load", error)
                emptyMap()
            }
            try {
                context.assets.open(BaseAssociationModel.ASSET).use { input ->
                    java.util.zip.GZIPInputStream(input).reader(Charsets.UTF_8).use {
                        BaseAssociationModel.readWithWordCompletions(it, wordCompletions)
                    }
                }
            } catch (error: Exception) {
                Log.e(TAG, "Bundled association model failed to load", error)
                null
            }
        }
        return baseModel != null
    }

    fun predict(context: String, afterWholeWord: Boolean = false): List<AssociationCandidate> =
        baseModel?.predict(context, BaseAssociationModel.MAX_PER_CONTEXT, afterWholeWord)
            ?.map { it.copy(score = it.score * BASE_WEIGHT) }.orEmpty()

    private companion object {
        const val TAG = "BundledAssociationModel"
        // Keep the established score scale used by the continuous association threshold.
        const val BASE_WEIGHT = 0.7f
    }
}
