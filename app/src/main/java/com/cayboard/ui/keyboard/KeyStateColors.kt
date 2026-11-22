package com.cayboard.ui.keyboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

internal const val PRESSED_KEY_TINT = 0.14f
internal fun keyStateColor(background: Color, foreground: Color, legacyStateColor: Color = background,
    pressed: Boolean = false, highlighted: Boolean = false): Color =
    if (pressed) lerp(background, foreground.copy(alpha = background.alpha), PRESSED_KEY_TINT) else legacyStateColor

internal fun keyHintColor(themeText: Color, legacyAlpha: Float, opacity: Float = 1f): Color =
    themeText.copy(alpha = (if (themeText.red < .3f && themeText.green < .3f && themeText.blue < .3f)
        maxOf(legacyAlpha, .75f) else legacyAlpha) * opacity.coerceIn(0f, 1f))
