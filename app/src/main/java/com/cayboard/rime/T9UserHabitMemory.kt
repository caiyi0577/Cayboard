package com.cayboard.rime

import android.content.Context
import org.json.JSONObject

/**
 * Small, offline preference layer for T9 candidate choices.
 *
 * Rime's userdb learns words after a complete commit, but the T9 processor has
 * its own digit path and the current APK does not connect that learning callback
 * to candidate ranking. This layer keeps the immediate per-digit preference in
 * the app user data, then lets the normal engine candidates remain the source of
 * truth. It never invents candidates and is deliberately skipped by sensitive
 * fields in the service.
 */
internal class T9UserHabitMemory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val lock = Any()
    private val counts = HashMap<String, MutableMap<String, Int>>()
    private var loaded = false

    private fun ensureLoaded() {
        if (loaded) return
        val root = runCatching { JSONObject(prefs.getString(KEY, "{}") ?: "{}") }.getOrDefault(JSONObject())
        root.keys().forEach { input ->
            val words = root.optJSONObject(input) ?: return@forEach
            val values = HashMap<String, Int>()
            words.keys().forEach { text -> values[text] = words.optInt(text, 0).coerceAtLeast(0) }
            if (values.isNotEmpty()) {
                val key = input.normalizedKey()
                val merged = counts.getOrPut(key) { HashMap() }
                values.forEach { (text, count) -> merged[text] = (merged[text] ?: 0) + count }
            }
        }
        loaded = true
    }

    /** Returns the same candidates, with remembered choices moved ahead stably. */
    fun rerank(
        input: String,
        candidates: Array<RimeCandidate>,
        durableOrder: List<String> = emptyList(),
    ): Array<RimeCandidate> = synchronized(lock) {
        ensureLoaded()
        val words = counts[input.normalizedKey()]
        if (words.isNullOrEmpty() && durableOrder.isEmpty()) {
            candidates
        } else {
            val durableRanks = durableOrder.withIndex().associate { it.value to it.index }
            candidates.withIndex().sortedWith(
                compareByDescending<IndexedValue<RimeCandidate>> { words?.get(it.value.text) ?: 0 }
                    .thenBy { durableRanks[it.value.text] ?: Int.MAX_VALUE }
                    .thenBy { it.index }
            ).map { it.value }.toTypedArray()
        }
    }

    /** Record an accepted full candidate choice. The count is bounded to avoid overflow. */
    fun remember(input: String, text: String) = synchronized(lock) {
        if (input.isBlank() || text.isBlank()) return@synchronized
        ensureLoaded()
        val key = input.normalizedKey()
        val words = counts.getOrPut(key) { HashMap() }
        words[text] = ((words[text] ?: 0) + 1).coerceAtMost(MAX_COUNT)
        val root = JSONObject()
        counts.forEach { (code, values) ->
            val json = JSONObject()
            values.forEach { (word, count) -> json.put(word, count) }
            root.put(code, json)
        }
        prefs.edit().putString(KEY, root.toString()).apply()
    }

    /** Store/query by physical T9 digits, even when Rime exposes the preedit as letters. */
    private fun String.normalizedKey(): String = canonicalT9Input(this)

    private companion object {
        const val PREFS = "t9_user_habits"
        const val KEY = "candidate_counts_v1"
        const val MAX_COUNT = 1_000_000
    }
}

internal fun canonicalT9Input(input: String): String = buildString(input.length) {
    for (ch in input.lowercase()) {
        when (ch) {
            in '2'..'9' -> append(ch)
            ':' -> append(ch)
            in 'a'..'c' -> append('2')
            in 'd'..'f' -> append('3')
            in 'g'..'i' -> append('4')
            in 'j'..'l' -> append('5')
            in 'm'..'o' -> append('6')
            in 'p'..'s' -> append('7')
            in 't'..'v' -> append('8')
            in 'w'..'z' -> append('9')
        }
    }
}
