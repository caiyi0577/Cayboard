package com.cayboard.engine

import android.content.Context
import org.json.JSONArray

internal data class LearnedCandidateChoice(
    val schemaId: String,
    val input: String,
    val candidate: EngineCandidate,
    val isT9: Boolean,
)

/** Stores one accepted choice per scheme and raw input code. */
internal class CandidateChoiceMemory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("t9_user_habits", Context.MODE_PRIVATE)

    fun previous(schemaId: String, input: String): EngineCandidate? {
        val record = prefs.getString(key(schemaId, input), null) ?: return null
        return runCatching {
            val data = JSONArray(record)
            EngineCandidate(data.getString(0), data.getString(1)).takeIf { it.text.isNotEmpty() }
        }.getOrNull()
    }

    fun remember(choice: LearnedCandidateChoice) {
        val (text, reading) = choice.candidate
        val record = JSONArray().put(text).put(reading).toString()
        prefs.edit().putString(key(choice.schemaId, choice.input), record).apply()
    }

    private fun key(schemaId: String, input: String) = "last-choice/${schemaId.length}:$schemaId$input"
}

/** The last choice owns the first slot. The dictionary orders the remaining words. */
internal fun orderPersonalizedCandidates(
    candidates: Array<EngineCandidate>,
    previous: EngineCandidate?,
    durableOrder: List<String>,
): Array<EngineCandidate> {
    if (previous == null && durableOrder.isEmpty()) return candidates
    val first = if (previous == null) candidates.firstOrNull()
        else candidates.firstOrNull { it.text == previous.text && it.comment == previous.comment } ?: previous
    first ?: return candidates
    val remaining = candidates.filter { it.text != first.text }
    val ordered = if (durableOrder.isEmpty()) remaining else {
        val rank = durableOrder.withIndex().associate { it.value to it.index }
        remaining.sortedBy { rank[it.text] ?: Int.MAX_VALUE }
    }
    return (listOf(first) + ordered).toTypedArray()
}

internal fun canonicalT9Input(input: String): String = buildString(input.length) {
    for (ch in input.lowercase()) {
        when (ch) {
            in '2'..'9', ':' -> append(ch)
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
