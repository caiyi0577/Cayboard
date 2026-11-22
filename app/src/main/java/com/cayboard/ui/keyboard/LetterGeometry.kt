package com.cayboard.ui.keyboard

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import com.cayboard.keyboard.LetterKeyRect
import com.cayboard.keyboard.encodeLetterNeighbors

/** Main-thread layout collector. Only immutable encoded maps cross into the input queue. */
class LetterGeometry(private val publish: (String) -> Unit) {
    private val keys = mutableMapOf<Char, LetterKeyRect>()
    fun place(text: String, rect: Rect) {
        val key = text.lowercase().singleOrNull()?.takeIf { it in 'a'..'z' } ?: return
        val measured = LetterKeyRect(rect.left, rect.top, rect.width, rect.height)
        if (keys.put(key, measured) != measured) publish(encodeLetterNeighbors(keys))
    }

    /** 当前各字母键的根坐标矩形（滑行识别用）。 */
    fun snapshot(): Map<Char, LetterKeyRect> = keys.toMap()
}

val LocalLetterGeometry = staticCompositionLocalOf<LetterGeometry?> { null }
