package com.cayboard.clipboard

/** Display-only preview. Paste, split and copy always receive the original record. */
internal fun clipboardRecordPreview(raw: String): String {
    val text = raw.trim()
    if (text.isEmpty()) return "空白文本"
    var end = minOf(text.length, 1200)
    if (end < text.length && Character.isLowSurrogate(text[end])) end--
    val result = StringBuilder()
    var blank = false
    text.substring(0,end).lineSequence().forEach { line ->
        val current = line.trimEnd()
        if (current.isNotBlank() || !blank) {
            if (result.isNotEmpty()) result.append('\n')
            result.append(current)
        }
        blank = current.isBlank()
    }
    return result.toString().trimEnd() + if (end < text.length) "…" else ""
}
