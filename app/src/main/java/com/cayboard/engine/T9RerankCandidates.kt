package com.cayboard.engine

/** Restrict promotion to ordinary Han candidates whose reading consumes every entered key. */
internal object T9RerankCandidates {
    private const val DIGITS = "22233344455566677778889999"

    fun eligible(result: EngineProcessResult): List<Int> {
        val input = result.inputText.lowercase().filterNot { it == '\'' || it == ' ' }
        val first = result.candidates.firstOrNull() ?: return emptyList()
        if (result.isAsciiMode || result.hasPrevPage || input.length !in 4..32 ||
            input.none { it in '2'..'9' } || input.any { it !in '2'..'9' && it !in 'a'..'z' } ||
            first.text.length !in 2..12
        ) return emptyList()
        val indexes = result.candidates.take(8).mapIndexedNotNull { index, candidate ->
            val syllables = candidate.comment.lowercase().trim().split(' ', '\'').filter(String::isNotEmpty)
            val reading = syllables.joinToString("")
            if (candidate.correction.isEmpty() && candidate.text.length == first.text.length &&
                candidate.text.all { it in '\u3400'..'\u9fff' } && syllables.size == candidate.text.length &&
                reading.length == input.length && reading.indices.all { i ->
                    val letter = reading[i]
                    letter in 'a'..'z' && (input[i] == letter || input[i] == DIGITS[letter - 'a'])
                }
            ) index else null
        }
        return indexes.takeIf { it.size >= 2 && it.first() == 0 }.orEmpty()
    }

    fun promote(result: EngineProcessResult, indexes: List<Int>, scores: FloatArray): EngineProcessResult {
        if (scores.size != indexes.size || scores.isEmpty() || scores.any { !it.isFinite() }) return result
        val best = scores.indices.maxBy { scores[it] }
        if (best == 0 || scores[best] - scores[0] < 0.1f) return result
        val promoted = indexes[best]
        val original = result.candidates
        return result.copy(candidates = Array(original.size) { index ->
            when {
                index == 0 -> original[promoted]
                index <= promoted -> original[index - 1]
                else -> original[index]
            }
        })
    }
}
