package com.cayboard.ui.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.cayboard.ui.theme.FixedKeyboardStyle
import kotlin.math.min
import kotlin.math.roundToInt

internal enum class FixedKeycapStyle {
    ORIGINAL, QWERTY, NUMBER_ROW, T9_GRID
}

internal val LocalFixedKeycapStyle = staticCompositionLocalOf { FixedKeycapStyle.ORIGINAL }

private val FixedKeycapCornerRadius = 4.dp
internal val FixedKeycapShape = RoundedCornerShape(FixedKeycapCornerRadius)

/** Fixed keycaps lower the face and its content inside the unchanged touch target. */
@Composable
internal fun Modifier.fixedSkinKeycap(label: String, pressed: Boolean): Modifier {
    val context = LocalContext.current
    val dark = true
    val keycapStyle = LocalFixedKeycapStyle.current
    // Light keys retain their palette, using the same travel with softer inset shading.
    if (!dark) {
        val fill = if (!pressed && keycapStyle == FixedKeycapStyle.NUMBER_ROW) FixedKeyboardStyle.scheme.keyBgLight
            else FixedKeyboardStyle.lightKeyFill(label, pressed)
        return clip(FixedKeycapShape).drawWithCache {
            val radius = CornerRadius(FixedKeycapCornerRadius.toPx())
            val travel = if (pressed) min(1.25.dp.toPx(), size.height * .035f) else 0f
            val faceSize = Size(size.width, size.height - travel)
            val faceOrigin = Offset(0f, travel)
            val insetPath = Path().apply { addRoundRect(RoundRect(Rect(faceOrigin, faceSize), radius)) }
            val topInset = 3.dp.toPx()
            val sideInset = 2.dp.toPx()
            val topShade = Brush.verticalGradient(listOf(Color.Black.copy(alpha = .18f), Color.Transparent),
                startY = travel, endY = travel + topInset)
            val sideShade = Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .10f), Color.Transparent),
                startX = 0f, endX = sideInset)
            onDrawWithContent {
                if (pressed) drawRoundRect(Color(0xFFADB8C3), cornerRadius = radius)
                drawRoundRect(fill, topLeft = faceOrigin, size = faceSize, cornerRadius = radius)
                drawRoundRect(Color(0xFFBEC6D0), topLeft = faceOrigin, size = faceSize,
                    cornerRadius = radius, style = Stroke(.4.dp.toPx()))
                if (pressed) {
                    clipPath(insetPath) {
                        drawRect(topShade, topLeft = faceOrigin, size = Size(size.width, topInset))
                        drawRect(sideShade, topLeft = faceOrigin, size = Size(sideInset, faceSize.height))
                        drawLine(Color.White.copy(alpha = .32f),
                            start = Offset(4.dp.toPx(), size.height - .5.dp.toPx()),
                            end = Offset(size.width - 4.dp.toPx(), size.height - .5.dp.toPx()), strokeWidth = .5.dp.toPx())
                    }
                    translate(top = travel) { this@onDrawWithContent.drawContent() }
                } else drawContent()
            }
        }
    }
    val atlas = FixedKeycapAtlas.get(context)
    val row = when (keycapStyle) {
        FixedKeycapStyle.ORIGINAL -> FixedKeyboardStyle.rowFor(label)
        FixedKeycapStyle.QWERTY -> if (label.length == 1 && label[0].lowercaseChar() in 'a'..'z') 1 else 3
        FixedKeycapStyle.NUMBER_ROW -> 0
        FixedKeycapStyle.T9_GRID -> 1
    }
    return clip(FixedKeycapShape).drawWithCache {
        val sourceX = row * 130
        val sourceY = if (pressed) 150 else 0
        val travel = if (pressed) min(1.25.dp.toPx(), size.height * .035f) else 0f
        val faceHeight = size.height - travel
        val scale = min(density / 3f, min(faceHeight / 114f, size.width / 106f))
        val dx = floatArrayOf(0f, 33f * scale, size.width - 33f * scale, size.width)
        val dy = floatArrayOf(travel, travel + 49f * scale, size.height - 45f * scale, size.height)
        val sx = intArrayOf(2, 35, 75, 108)
        val sy = intArrayOf(1, 50, 70, 115)
        val radius = CornerRadius(FixedKeycapCornerRadius.toPx())
        val insetPath = Path().apply { addRoundRect(RoundRect(Rect(0f, travel, size.width, size.height), radius)) }
        val topInset = 3.dp.toPx()
        val sideInset = 2.dp.toPx()
        val topShade = Brush.verticalGradient(listOf(Color.Black.copy(alpha = .36f), Color.Transparent),
            startY = travel, endY = travel + topInset)
        val sideShade = Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .22f), Color.Transparent),
            startX = 0f, endX = sideInset)
        onDrawWithContent {
            if (pressed) drawRoundRect(Color(0xFF141A1D), cornerRadius = radius)
            for (y in 0..2) for (x in 0..2) {
                val left = dx[x].roundToInt(); val top = dy[y].roundToInt()
                val width = dx[x + 1].roundToInt() - left
                val height = dy[y + 1].roundToInt() - top
                if (width > 0 && height > 0) drawImage(atlas,
                    srcOffset = IntOffset(sourceX + sx[x], sourceY + sy[y]),
                    srcSize = IntSize(sx[x + 1] - sx[x], sy[y + 1] - sy[y]),
                    dstOffset = IntOffset(left, top), dstSize = IntSize(width, height),
                    filterQuality = FilterQuality.Low)
            }
            if (pressed) {
                clipPath(insetPath) {
                    drawRect(topShade, topLeft = Offset(0f, travel), size = Size(size.width, topInset))
                    drawRect(sideShade, topLeft = Offset(0f, travel), size = Size(sideInset, faceHeight))
                    drawLine(Color.White.copy(alpha = .07f),
                        start = Offset(4.dp.toPx(), size.height - .5.dp.toPx()),
                        end = Offset(size.width - 4.dp.toPx(), size.height - .5.dp.toPx()), strokeWidth = .5.dp.toPx())
                }
                translate(top = travel) { this@onDrawWithContent.drawContent() }
            } else drawContent()
        }
    }
}

@Composable
internal fun fixedSkinContentColor(label: String): androidx.compose.ui.graphics.Color {
    val scheme = FixedKeyboardStyle.scheme
    val dark = true
    return if (FixedKeyboardStyle.rowFor(label) == 3) {
        if (dark) scheme.specialKeyTextColorDark!! else scheme.specialKeyTextColorLight!!
    } else if (dark) scheme.keyTextColorDark else scheme.keyTextColorLight
}
