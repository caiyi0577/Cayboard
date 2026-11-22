package com.cayboard.translate

import android.content.Context
import com.cayboard.settings.MarketVersionStore
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object BundledTranslationModel {
    const val NAME = "Hy-MT2-1.8B"
    const val VERSION = "2（1.25bit）"
    const val SIZE = 461860800L
    const val SHA256 = "cc497fe8f033b52b3b8b00a7669e9661435432f9d4cd43f7ed24400c01507a93"
    const val ASSET = "builtin-translation/Hy-MT2-1.8B-1.25Bit.gguf"
    const val SIZE_LABEL = "440.5 MiB"
    private val _phase = MutableStateFlow("preparing")
    val phase = _phase.asStateFlow()
    @Volatile var failure: String? = null
        private set
    fun file(context: Context) = File(context.filesDir, "translation/Hy-MT2-1.8B-1.25Bit.gguf")

    /** Restore the ready state after process restart when the verified model is already present. */
    fun refresh(context: Context): Boolean {
        val target = file(context)
        val prefs = context.getSharedPreferences("builtin_translation_model", Context.MODE_PRIVATE)
        val ready = target.isFile && target.length() == SIZE && prefs.getString("sha256", null) == SHA256 &&
            prefs.getLong("modified", -1) == target.lastModified()
        if (ready) { _phase.value = "ready"; failure = null }
        return ready
    }

    /** Only call from a background worker. Publish a verified complete file before retiring the old pair. */
    fun prepare(context: Context): File = prepareVerified(context, SIZE, SHA256) { context.assets.open(ASSET) }
    @Synchronized internal fun prepareVerified(context: Context, expectedSize: Long, expectedSha: String,
        openAsset: () -> java.io.InputStream): File {
        val target = file(context)
        val prefs = context.getSharedPreferences("builtin_translation_model", Context.MODE_PRIVATE)
        try {
            if (target.isFile && target.length() == expectedSize && prefs.getString("sha256", null) == expectedSha &&
                prefs.getLong("modified", -1) == target.lastModified()) {
                _phase.value = "ready"; failure = null; cleanupRetiredModels(context); return target
            }
            _phase.value = "preparing"
            target.parentFile!!.mkdirs()
            val pending = File(target.parentFile, target.name + ".preparing")
            try {
                val hash = MessageDigest.getInstance("SHA-256")
                var size = 0L
                openAsset().use { input -> pending.outputStream().use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count); hash.update(buffer, 0, count); size += count
                    }
                } }
                check(size == expectedSize && hash.digest().joinToString("") { "%02x".format(it) } == expectedSha) { "内置翻译模型校验失败" }
                Files.move(pending.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                prefs.edit().putString("sha256", expectedSha).putLong("modified", target.lastModified()).commit()
            } finally { if (pending.exists()) pending.delete() }
            failure = null; _phase.value = "ready"
            cleanupRetiredModels(context)
            return target
        } catch (error: Exception) {
            failure = error.message ?: "内置翻译模型准备失败"; _phase.value = "error"; throw error
        }
    }
    private fun cleanupRetiredModels(context: Context) {
        val root = File(context.filesDir, "models").canonicalFile
        for (id in listOf("translate-zh-en", "translate-en-zh")) {
            val old = File(root, id)
            if (old.canonicalFile.parentFile == root && old.exists()) old.deleteRecursively()
            MarketVersionStore.removeModelVersion(context, id)
        }
    }
}
