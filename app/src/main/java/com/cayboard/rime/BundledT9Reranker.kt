package com.cayboard.rime

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Offline 6x256 candidate scorer. Model loading never blocks the input queue. */
internal class BundledT9Reranker(context: Context) : AutoCloseable {
    private val assets = context.applicationContext.assets
    private val started = AtomicBoolean()
    private val lock = ReentrantLock()
    @Volatile private var closed = false
    private var model: Model? = null

    fun warmUp(scope: CoroutineScope) {
        if (closed || !started.compareAndSet(false, true)) return
        scope.launch(Dispatchers.Default) {
            lock.withLock {
                try {
                    if (!closed) model = load()
                } catch (e: Exception) {
                    Log.w(TAG, "Bundled reranker unavailable", e)
                } catch (e: LinkageError) {
                    Log.w(TAG, "Bundled reranker runtime unavailable", e)
                } finally {
                    if (closed) release()
                }
            }
        }
    }

    fun rerank(result: RimeProcessResult, leftContext: String): RimeProcessResult {
        val indexes = T9RerankCandidates.eligible(result)
        if (indexes.isEmpty() || closed || !lock.tryLock()) return result
        try {
            val scorer = model ?: return result
            val candidates = indexes.map { result.candidates[it].text }
            return T9RerankCandidates.promote(result, indexes, scorer.score(leftContext, candidates))
        } catch (e: Exception) {
            Log.w(TAG, "Bundled reranker disabled for this session", e)
            release()
            return result
        } finally {
            if (closed) release()
            lock.unlock()
        }
    }

    override fun close() {
        closed = true
        if (!lock.tryLock()) return
        try { release() } finally { lock.unlock() }
    }

    private fun release() {
        runCatching { model?.close() }
        model = null
    }

    private fun load(): Model {
        fun json(name: String) = JSONObject(assets.open("$ASSETS/$name").bufferedReader().use { it.readText() })
        val config = json("model.json")
        val tokens = json("vocab.json").getJSONArray("tokens")
        val vocab = buildMap {
            for (i in 0 until tokens.length()) {
                val token = tokens.getString(i)
                if (token.codePointCount(0, token.length) == 1) put(token.codePointAt(0), i.toLong())
            }
        }
        val env = OrtEnvironment.getEnvironment()
        val weights = assets.open("$ASSETS/lm_step.ort").use { it.readBytes() }
        return OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(1)
            options.addConfigEntry("session.intra_op.allow_spinning", "0")
            options.addConfigEntry("session.inter_op.allow_spinning", "0")
            val bos = config.getLong("bos")
            val unk = config.getLong("unk")
            Model(env, env.createSession(weights, options), vocab, bos, unk)
        }
    }

    private class Model(
        val env: OrtEnvironment,
        val session: OrtSession,
        val vocab: Map<Int, Long>,
        val bos: Long,
        val unk: Long,
    ) {
        private var cachedContext: List<Long>? = null
        private var contextResult: OrtSession.Result? = null

        private fun encode(text: String) = text.codePoints().toArray().map { vocab[it] ?: unk }

        fun close() {
            try { contextResult?.close() } finally { session.close() }
        }

        private fun run(
            nodes: LongArray, positions: LongArray, visible: ByteArray, visibleWidth: Int,
            bodyIds: LongArray, steps: LongArray, lengths: LongArray, width: Int,
            previous: OrtSession.Result? = null,
        ): OrtSession.Result {
            val inputs = LinkedHashMap<String, OnnxTensor>()
            fun longs(name: String, values: LongArray, vararg shape: Long) {
                inputs[name] = OnnxTensor.createTensor(env, LongBuffer.wrap(values), shape)
            }
            fun empty(name: String, vararg shape: Long) {
                inputs[name] = OnnxTensor.createTensor(env, FloatBuffer.allocate(0), shape)
            }
            try {
                if (previous == null) {
                    empty("cache_k", 6, 4, 0, 64)
                    empty("cache_v", 6, 4, 0, 64)
                    empty("cache_h", 0, 256)
                    empty("cache_z", 0)
                }
                val rows = lengths.size.toLong()
                longs("nodes", nodes, nodes.size.toLong())
                longs("positions", positions, positions.size.toLong())
                inputs["visible"] = OnnxTensor.createTensor(env, ByteBuffer.wrap(visible),
                    longArrayOf(nodes.size.toLong(), visibleWidth.toLong()), OnnxJavaType.BOOL)
                longs("candidates", bodyIds, rows, width.toLong())
                longs("steps", steps, rows, width.toLong())
                longs("lengths", lengths, rows)
                if (previous == null) return session.run(inputs)
                val cache = listOf("cache_k", "cache_v", "cache_h", "cache_z")
                    .mapIndexed { index, name -> name to (previous[index + 1] as OnnxTensor) }.toMap()
                return session.run(inputs + cache, setOf("scores"))
            } finally {
                inputs.values.forEach { it.close() }
            }
        }

        private fun encodeContext(context: List<Long>): OrtSession.Result {
            if (context == cachedContext) contextResult?.let { return it }
            val size = context.size
            val visible = ByteArray(size * size)
            context.indices.forEach { node -> visible.fill(1, node * size, node * size + node + 1) }
            val result = run(context.toLongArray(), LongArray(size) { it.toLong() }, visible, size,
                longArrayOf(bos), longArrayOf(0), longArrayOf(1), 1)
            contextResult?.close()
            contextResult = result
            cachedContext = context
            return result
        }

        fun score(leftContext: String, candidates: List<String>): FloatArray {
            val context = listOf(bos) + encode(leftContext.takeLast(16))
            val bodies = candidates.map(::encode)
            val contextCache = encodeContext(context)
            // Each candidate can only see its own prefixes and the cached context.
            val prefixes = LinkedHashMap<List<Long>, Int>()
            bodies.forEach { body ->
                for (length in 1 until body.size) {
                    val prefix = body.subList(0, length)
                    prefixes.getOrPut(prefix) { context.size + prefixes.size }
                }
            }
            val size = context.size + prefixes.size
            val positions = LongArray(prefixes.size)
            val nodes = prefixes.keys.map { it.last() }.toLongArray()
            val visible = ByteArray(prefixes.size * size)
            for ((prefix, node) in prefixes) {
                val row = node - context.size
                positions[row] = (context.size + prefix.size - 1).toLong()
                visible.fill(1, row * size, row * size + context.size)
                for (length in 1..prefix.size) {
                    visible[row * size + prefixes.getValue(prefix.subList(0, length))] = 1
                }
            }
            val width = bodies.maxOf { it.size }
            val bodyIds = LongArray(bodies.size * width)
            val steps = LongArray(bodyIds.size)
            for ((row, body) in bodies.withIndex()) {
                for ((column, token) in body.withIndex()) {
                    val i = row * width + column
                    bodyIds[i] = token
                    steps[i] = (if (column == 0) context.lastIndex else prefixes.getValue(body.subList(0, column))).toLong()
                }
            }
            return run(nodes, positions, visible, size, bodyIds, steps,
                bodies.map { it.size.toLong() }.toLongArray(), width, contextCache).use { it[0].value as FloatArray }
        }
    }

    companion object {
        private const val ASSETS = "builtin-t9-reranker"
        private const val TAG = "BundledT9Reranker"

        fun isBundled(context: Context): Boolean =
            runCatching { context.assets.open("$ASSETS/lm_step.ort").use { true } }.getOrDefault(false)
    }
}
