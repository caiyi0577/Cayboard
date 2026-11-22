package com.cayboard.speech

import android.content.Context
import java.io.File
import java.security.MessageDigest
import org.json.JSONArray

/** One fixed, packaged recognizer. Preparation never uses a network or a model picker. */
object FixedSpeechModel {
    const val NAME = "X-ASR INT8"
    const val VERSION = "2026-06-03"
    @Volatile private var prepared = false

    @Synchronized fun prepare(context: Context) {
        if (prepared) return
        val root = File(context.filesDir, "models/${SpeechModelCatalog.XASR}").apply { mkdirs() }
        val manifest = JSONArray(context.assets.open("builtin-speech/manifest.json").bufferedReader().use { it.readText() })
        fun digest(file: File): String {
            val hash = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(128 * 1024)
                while (true) { val count = input.read(buffer); if (count < 0) break; hash.update(buffer, 0, count) }
            }
            return hash.digest().joinToString("") { "%02x".format(it) }
        }
        for (index in 0 until manifest.length()) {
            val entry = manifest.getJSONObject(index)
            val name = entry.getString("name")
            require(name.matches(Regex("[A-Za-z0-9_.-]+")))
            val target = File(root, name)
            val expected = entry.getString("sha256")
            if (target.isFile && target.length() == entry.getLong("bytes") && digest(target) == expected) continue
            val temporary = File(root, "$name.installing")
            try {
                context.assets.open("builtin-speech/xasr/$name").use { input -> temporary.outputStream().use { input.copyTo(it) } }
                check(temporary.length() == entry.getLong("bytes") && digest(temporary) == expected) { "语音模型校验失败" }
                check(temporary.renameTo(target)) { "语音模型准备失败" }
            } finally { temporary.delete() }
        }
        // Only retire former application-owned voice model directories after X-ASR is verified.
        for (id in listOf(SpeechModelCatalog.SENSEVOICE, SpeechModelCatalog.PARAFORMER)) {
            File(context.filesDir, "models/$id").deleteRecursively()
        }
        context.getSharedPreferences("online_speech_api", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("asr_model", Context.MODE_PRIVATE).edit().clear().apply()
        prepared = true
    }
}
