package com.cayboard.settings

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ln

/** DAG / Viterbi adapted from fxsjy/jieba (c) 2013 Sun Junyi, MIT; license in bundled NOTICE. */
/** Memory-mapped CSR prefix trie and Jieba B/M/E/S model. Immutable and safe for concurrent readers. */
internal class CompactClipboardLexicon(buffer: ByteBuffer) {
    private val data = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
    private val nodes = data.getInt(8)
    private val edges = data.getInt(12)
    private val emissions = data.getInt(16)
    private val maxWord = data.getInt(20)
    private val unknown = -ln(data.getDouble(24))
    private val edgeBase = 32 + nodes * 12
    private val emissionBase = edgeBase + edges * 8
    private val startBase = emissionBase + emissions * 20
    init {
        require(data.getInt(0) == 0x31475343 && data.getInt(4) == 1) { "Invalid clipboard lexicon" }
        require(nodes > 0 && edges == nodes - 1 && maxWord in 1..128 && startBase + 80 == data.limit())
    }
    private fun frequency(node: Int) = data.getFloat(32 + node * 12 + 8).toDouble()
    private fun child(node: Int, codePoint: Int): Int {
        var low = data.getInt(32 + node * 12)
        var high = low + data.getInt(32 + node * 12 + 4) - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val cp = data.getInt(edgeBase + mid * 8)
            if (cp == codePoint) return data.getInt(edgeBase + mid * 8 + 4)
            if (cp < codePoint) low = mid + 1 else high = mid - 1
        }
        return -1
    }
    private fun known(points: IntArray): Boolean {
        var node = 0
        for (cp in points) { node = child(node, cp); if (node < 0) return false }
        return frequency(node) != 0.0
    }
    fun splitHan(text: String): List<String> {
        val points = text.codePoints().toArray()
        val n = points.size
        val score = DoubleArray(n + 1)
        val next = IntArray(n)
        for (i in n - 1 downTo 0) {
            score[i] = unknown + score[i + 1]; next[i] = i + 1
            var node = 0
            for (j in i until minOf(n, i + maxWord)) {
                node = child(node, points[j]); if (node < 0) break
                val log = frequency(node)
                if (log != 0.0 && log + score[j + 1] >= score[i]) {
                    score[i] = log + score[j + 1]; next[i] = j + 1
                }
            }
        }
        val result = ArrayList<String>()
        var i = 0
        while (i < n) {
            if (next[i] > i + 1) { result.add(String(points, i, next[i] - i)); i = next[i]; continue }
            val begin = i
            do { i++ } while (i < n && next[i] == i + 1)
            val run = points.copyOfRange(begin, i)
            if (run.size == 1 || known(run)) run.forEach { result.add(String(Character.toChars(it))) }
            else result.addAll(hmm(run))
        }
        return result
    }
    private fun emissionOffset(cp: Int): Int {
        var low = 0; var high = emissions - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val value = data.getInt(emissionBase + mid * 20)
            if (value == cp) return emissionBase + mid * 20 + 4
            if (value < cp) low = mid + 1 else high = mid - 1
        }
        return -1
    }
    private fun hmm(points: IntArray): List<String> {
        val previous = IntArray(points.size * 4)
        var scores = DoubleArray(4)
        for (i in points.indices) {
            val offset = emissionOffset(points[i])
            val nextScores = DoubleArray(4) { -1e100 }
            for (state in 0..3) {
                val emit = if (offset < 0) -1e30 else data.getFloat(offset + state * 4).toDouble()
                if (i == 0) nextScores[state] = data.getFloat(startBase + state * 4) + emit
                else for (prior in 0..3) {
                    val transition = data.getFloat(startBase + 16 + (prior * 4 + state) * 4)
                    val value = scores[prior] + transition + emit
                    if (value >= nextScores[state]) { nextScores[state] = value; previous[i * 4 + state] = prior }
                }
            }
            scores = nextScores
        }
        var state = if (scores[2] > scores[3]) 2 else 3
        val path = IntArray(points.size)
        for (i in points.lastIndex downTo 0) { path[i] = state; state = previous[i * 4 + state] }
        val result = ArrayList<String>(); var begin = 0
        for (i in points.indices) if (path[i] == 2 || path[i] == 3) {
            result.add(String(points, begin, i - begin + 1)); begin = i + 1
        }
        if (begin < points.size) result.add(String(points, begin, points.size - begin))
        return result
    }
}
