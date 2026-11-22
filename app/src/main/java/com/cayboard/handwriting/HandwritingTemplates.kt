package com.cayboard.handwriting

import android.content.Context
import android.util.Log
import com.cayboard.settings.InputLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


object HandwritingTemplates {
    private const val TAG = "HandwritingTemplates"
    private const val DIR = "handwriting_builtin_cache"
    
    const val OCH_TOP_N = 320

    
    enum class Script {
        LATIN;
        fun available(context: Context): Boolean = FormulaRecognizer.isAvailable(context)
        companion object {
            fun forLanguage(language: InputLanguage): Script? = if (language == InputLanguage.ENGLISH) LATIN else null
        }
    }

    internal class Feature(val dense: FloatArray? = null, val idx: IntArray? = null, val vals: FloatArray? = null) {
        fun dot(o: Feature): Float {
            if (dense != null && o.dense != null) {
                var d = 0f
                for (i in 0 until minOf(dense.size, o.dense.size)) d += dense[i] * o.dense[i]
                return d
            }
            val a = idx ?: return 0f
            val av = vals ?: return 0f
            val b = o.idx ?: return 0f
            val bv = o.vals ?: return 0f
            var i = 0
            var j = 0
            var d = 0f
            while (i < a.size && j < b.size) when {
                a[i] == b[j] -> { d += av[i] * bv[j]; i++; j++ }
                a[i] < b[j] -> i++
                else -> j++
            }
            return d
        }

        companion object {
            
            fun fromProbabilities(pairs: List<Pair<Int, Float>>): Feature? {
                val kept = pairs.filter { it.second > 0f && it.second.isFinite() }.distinctBy { it.first }.sortedBy { it.first }
                if (kept.isEmpty()) return null
                val v = FloatArray(kept.size) { kotlin.math.sqrt(kept[it].second) }
                val norm = kotlin.math.sqrt(v.fold(0f) { acc, x -> acc + x * x }).coerceAtLeast(1e-9f)
                return Feature(idx = IntArray(kept.size) { kept[it].first }, vals = FloatArray(v.size) { v[it] / norm })
            }
        }
    }

    private class Entry(val label: String, val feat: Feature, val source: String)


    
    private val defaults = HashMap<Script, List<Entry>>()
    
    private const val LATIN_DEFAULT_STROKES = "handwriting/latin_default_strokes.json"
    private const val LATIN_DEFAULT_CACHE = "latin_default_v1.json"
    

    

    private fun dir(context: Context) = File(context.filesDir, DIR).apply { mkdirs() }

    private fun Feature.toJson(o: JSONObject): JSONObject {
        dense?.let { d -> o.put("v", JSONArray().also { a -> d.forEach { a.put(Math.round(it * 10000) / 10000.0) } }) }
        val i = idx
        val v = vals
        if (i != null && v != null) {
            o.put("i", JSONArray().also { a -> i.forEach { a.put(it) } })
            o.put("v", JSONArray().also { a -> v.forEach { a.put(Math.round(it * 10000) / 10000.0) } })
        }
        return o
    }

    private fun featureFromJson(o: JSONObject): Feature {
        val v = o.getJSONArray("v")
        val values = FloatArray(v.length()) { v.getDouble(it).toFloat() }
        val i = o.optJSONArray("i") ?: return Feature(dense = values)
        return Feature(idx = IntArray(i.length()) { i.getInt(it) }, vals = values)
    }

    private fun feature(context: Context, script: Script, strokes: List<List<Pair<Float, Float>>>): Feature? = when (script) {
        Script.LATIN -> FormulaRecognizer.embed(context, strokes)?.let { Feature(dense = it) }
    }

    




    





    fun count(context: Context, script: Script): Int = synchronized(this) { defaults[script]?.size ?: 0 }

    
    fun ensureDefaults(context: Context, script: Script): Unit = synchronized(defaultLock) { prepareDefaults(context, script) }

    private val defaultLock = Any()

    private fun prepareDefaults(context: Context, script: Script) {
        if (synchronized(this) { defaults.containsKey(script) }) return
        if (script.available(context)) prepareLatinDefaults(context)
    }

    
    private fun prepareLatinDefaults(context: Context) {
        if (!Script.LATIN.available(context)) return
        val encoder = File(FormulaRecognizer.dir(context), "formula_encoder.onnx")
        val stamp = "${encoder.path}:${encoder.length()}:${encoder.lastModified()}"
        val cacheFile = File(dir(context), LATIN_DEFAULT_CACHE)
        val cached = runCatching {
            val o = JSONObject(cacheFile.readText())
            if (o.getString("model") != stamp) null else {
                val arr = o.getJSONArray("t")
                (0 until arr.length()).map { i -> arr.getJSONObject(i).let { e -> Entry(e.getString("l"), featureFromJson(e), "builtin") } }
            }
        }.getOrNull()
        val list = cached ?: runCatching {
            val chars = JSONObject(context.assets.open(LATIN_DEFAULT_STROKES).bufferedReader(Charsets.UTF_8).use { it.readText() })
                .getJSONObject("chars")
            val out = mutableListOf<Entry>()
            for (label in chars.keys()) {
                val variants = chars.getJSONArray(label)
                for (i in 0 until variants.length()) {
                    val strokes = variants.getJSONArray(i).let { st ->
                        (0 until st.length()).map { j ->
                            val pts = st.getJSONArray(j)
                            (0 until pts.length()).map { k -> pts.getJSONArray(k).let { p -> p.getDouble(0).toFloat() to p.getDouble(1).toFloat() } }
                        }
                    }
                    feature(context, Script.LATIN, strokes)?.let { out += Entry(label, it, "builtin") }
                }
            }
            if (out.isEmpty()) return
            val arr = JSONArray()
            for (e in out) arr.put(e.feat.toJson(JSONObject().put("l", e.label)))
            val tmp = File(cacheFile.path + ".tmp")
            tmp.writeText(JSONObject().put("model", stamp).put("t", arr).toString())
            if (!tmp.renameTo(cacheFile)) { tmp.copyTo(cacheFile, overwrite = true); tmp.delete() }
            out
        }.onFailure { Log.w(TAG, "builtin latin templates failed", it) }.getOrNull() ?: return
        synchronized(this) { defaults[Script.LATIN] = list }
    }

    


    private fun parseStrokes(arr: JSONArray?): List<List<Pair<Float, Float>>>? {
        arr ?: return null
        val out = mutableListOf<List<Pair<Float, Float>>>()
        for (i in 0 until arr.length()) {
            val pts = arr.getJSONObject(i).optJSONArray("points") ?: continue
            val stroke = (0 until pts.length()).map { j ->
                val p = pts.getJSONArray(j)
                p.getDouble(0).toFloat() to p.getDouble(1).toFloat()
            }
            if (stroke.isNotEmpty()) out += stroke
        }
        return out.ifEmpty { null }
    }


    
    fun recognize(context: Context, script: Script, strokes: List<List<Pair<Float, Float>>>, topK: Int = 8): List<Pair<String, Float>> {
        val builtin = synchronized(this) { defaults[script].orEmpty() }
        if (builtin.isEmpty()) return emptyList()
        val query = feature(context, script, strokes) ?: return emptyList()
        val ranked = rank(query, builtin.map { it.label to it.feat }, topK)
        return ranked
    }

    
    internal fun rank(query: Feature, builtin: List<Pair<String, Feature>>, topK: Int): List<Pair<String, Float>> {
        val best = HashMap<String, Float>()
        for ((label, feat) in builtin) {
            val s = query.dot(feat)
            if (s > (best[label] ?: -2f)) best[label] = s
        }
        return best.entries.sortedByDescending { it.value }.take(topK)
            .map { it.key to ((it.value + 1f) / 2f).coerceIn(0.0001f, 1f) }
    }

    

    fun isUsable(context: Context, script: Script): Boolean = script.available(context) && count(context, script) > 0

}
