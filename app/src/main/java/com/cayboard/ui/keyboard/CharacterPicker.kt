package com.cayboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/** Display and hit testing share exactly the same cells, including screen-edge clamping. */
internal data class CharacterPickerLayout(val bounds: Rect, val columns: Int, val cellWidth: Float,
    val cellHeight: Float, val keyBounds: Rect) {
    fun selection(point: Offset, previous: Int, count: Int): Int {
        if (bounds.contains(point)) {
            val column = floor((point.x - bounds.left) / cellWidth).toInt()
            val row = floor((point.y - bounds.top) / cellHeight).toInt()
            return (row * columns + column).takeIf { it in 0 until count } ?: -1
        }
        // The corridor between the held key and panel keeps the current choice.
        val corridor = Rect(minOf(bounds.left, keyBounds.left), bounds.bottom,
            maxOf(bounds.right, keyBounds.right), keyBounds.bottom + cellHeight * .5f)
        return if (corridor.contains(point)) previous else -1
    }
}

internal fun characterPickerLayout(key: Rect, windowWidth: Float, density: Float, count: Int,
    minimumTop: Float = 0f): CharacterPickerLayout {
    val total = count.coerceAtLeast(1)
    val availableWidth = (windowWidth - 16f * density).coerceAtLeast(1f)
    val maxColumns = floor(availableWidth / (60f * density)).toInt().coerceAtLeast(1)
    val rows = (total + maxColumns - 1) / maxColumns
    val columns = (total + rows - 1) / rows
    val cellWidth = min(60f * density, availableWidth / columns)
    val cellHeight = 64f * density
    val width = columns * cellWidth
    val height = rows * cellHeight
    val left = (key.center.x - width / 2).coerceIn(8f * density,
        maxOf(8f * density, windowWidth - width - 8f * density))
    val top = (key.top - height - 12f * density).coerceAtLeast(minimumTop)
    return CharacterPickerLayout(Rect(left, top, left + width, top + height), columns, cellWidth, cellHeight, key)
}

internal fun keyCharacterChoices(text: String, group: String?, badge: String?, upper: String?, lower: String?,
    extras: List<String>): List<String> = buildList {
    badge?.takeIf { it.length == 1 }?.let(::add)
    val main = group ?: text
    if (main.all { it.lowercaseChar() in 'a'..'z' } && main.length <= 4) {
        text.forEach { add(it.toString()) }
        text.forEach { add((if (it.isLowerCase()) it.uppercaseChar() else it.lowercaseChar()).toString()) }
    }
    else if (text.length == 1) add(text)
    upper?.takeIf { it.isNotBlank() }?.let(::add)
    lower?.takeIf { it.isNotBlank() }?.let(::add)
    addAll(extras)
}.filter { it.isNotBlank() }.distinct()

@Composable
internal fun CharacterPickerPanel(items: List<String>, selected: Int, layout: CharacterPickerLayout,
    background: Color, foreground: Color, accent: Color, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val rows = (items.size + layout.columns - 1) / layout.columns
    Column(modifier.width(with(density) { layout.bounds.width.toDp() })
        .background(background, RoundedCornerShape(16.dp))
        .border(1.dp, foreground.copy(alpha = .18f), RoundedCornerShape(16.dp))
        .testTag("character-picker")) {
        repeat(rows) { row ->
            Row {
                repeat(layout.columns) { column ->
                    val index = row * layout.columns + column
                    Box(Modifier.weight(1f).height(with(density) { layout.cellHeight.toDp() })
                        .padding(6.dp)
                        .background(if (index == selected) accent else Color.Transparent, RoundedCornerShape(10.dp))
                        .testTag("character-choice-$index"), contentAlignment = Alignment.Center) {
                        items.getOrNull(index)?.let { Text(it, color = if (index == selected)
                            (if (accent.luminance() > .35f) Color(0xFF17252B) else Color.White) else foreground,
                            fontSize = 24.sp, maxLines = 1) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CharacterPickerPopup(items: List<String>, selected: Int, layout: CharacterPickerLayout,
    background: Color, foreground: Color, accent: Color) {
    val view = LocalView.current
    val placement = remember(view) { PopupWindowPlacement(view) }
    val correction = PopupWindowCorrection.offsetFor(placement.origin)
    val provider = remember(layout, correction) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
                layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset = placement.place(
                IntOffset((viewLocation(view).x + layout.bounds.left).roundToInt(),
                    (viewLocation(view).y + layout.bounds.top).roundToInt()))
        }
    }
    Popup(popupPositionProvider = provider, properties = DisplayOnlyPopupProperties) {
        VerifyPopupWindowPlacement(placement)
        CharacterPickerPanel(items, selected, layout, background, foreground, accent)
    }
}

private fun viewLocation(view: android.view.View): Offset {
 val location = IntArray(2); view.getLocationInWindow(location)
 return Offset(location[0].toFloat(), location[1].toFloat())
}

/** IME popups may extend above the keyboard; keep the panel above its key while staying on screen. */
internal fun characterPickerTopLimit(view: android.view.View, density: Float): Float {
    val location = IntArray(2)
    view.getLocationOnScreen(location)
    return 8f * density - location[1]
}
