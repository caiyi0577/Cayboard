package com.cayboard.settings

import android.content.Context
import android.icu.text.BreakIterator
import android.util.Log
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Dedicated offline Jieba DAG/HMM segmentation. Never reads or locks the input engine's Rime dictionaries. */
object ClipboardWordSegmenter {
    @Volatile private var lexicon: CompactClipboardLexicon? = null
    internal var loadCount = 0
        private set
    private const val ASSET = "clipboard-segmentation/jieba.cseg"
    private fun isHan(cp: Int) = Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN
    @Synchronized private fun dictionary(context: Context): CompactClipboardLexicon {
        lexicon?.let { return it }
        val started = System.nanoTime()
        val bytes = try {
            context.assets.openFd(ASSET).use { fd ->
                fd.createInputStream().use { stream ->
                    stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
        } catch (_: java.io.IOException) { ByteBuffer.wrap(context.assets.open(ASSET).use { it.readBytes() }) }
        return CompactClipboardLexicon(bytes).also {
            lexicon = it; loadCount++
            Log.d("ClipboardSplit", "Lexicon mapped in ${(System.nanoTime()-started)/1_000_000} ms")
        }
    }
    suspend fun prewarm(context: Context) = withContext(Dispatchers.IO) {
        runCatching { dictionary(context.applicationContext) }.onFailure { Log.w("ClipboardSplit", "Lexicon preload unavailable", it) }
        Unit
    }
    suspend fun split(context: Context, text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val hasHan = text.codePoints().anyMatch(::isHan)
        val dict = if (hasHan) withContext(Dispatchers.IO) { dictionary(context.applicationContext) } else null
        return withContext(Dispatchers.Default) {
            val result = ArrayList<String>()
            for (run in quickSplit(text)) {
                ensureActive()
                if (dict != null && isHan(run.codePointAt(0))) result.addAll(dict.splitHan(run)) else result.add(run)
            }
            result
        }
    }
    /** Immediate original-text preview, also the fast path for numbers and Latin text. */
    fun quickSplit(text: String): List<String> {
        val result = ArrayList<String>()
        val graphemes = characters(text)
        val pending = StringBuilder(); var previous = -1
        fun flush() { if (pending.isNotEmpty()) { result.add(pending.toString()); pending.setLength(0) } }
        for (part in graphemes) {
            val cp = part.codePointAt(0)
            val kind = when {
                isHan(cp) -> 0
                part.all { it.isWhitespace() } -> 1
                Character.isLetterOrDigit(cp) -> 2
                else -> 3
            }
            if (kind != previous || kind == 3) flush()
            pending.append(part); previous = kind
        }
        flush(); return result
    }
    /** ICU grapheme boundaries keep emoji families, skin tones, flags and supplementary Han intact. */
    fun characters(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
        iterator.setText(text)
        val result = ArrayList<String>(); var begin = iterator.first(); var end = iterator.next()
        while (end != BreakIterator.DONE) { result.add(text.substring(begin,end)); begin=end; end=iterator.next() }
        return result
    }
}
