package com.cayboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable internal fun Modifier.keyboardPanelBackground(fallback: Color): Modifier = background(fallback)
@Composable internal fun Modifier.keyboardOverlayBackground(background: Color): Modifier = keyboardPanelBackground(background)

internal data class KeyboardCardColors(val card: Color, val text: Color, val scrim: Color)

/**
 * Keyboard palette, not the Material surface: the card looks like the keys of every theme (a white
 * Material surface showed on dark iOS). It covers other content, so it stays opaque even on glass.
 */
@Composable
internal fun keyboardCardColors(): KeyboardCardColors {
    val colors = MaterialTheme.colorScheme
    val palette = com.cayboard.ui.theme.LocalKeyboardPalette.current
    return KeyboardCardColors(
        card = palette?.key?.copy(alpha = 1f) ?: colors.surfaceContainerHigh,
        text = palette?.text ?: colors.onSurface,
        scrim = colors.scrim.copy(alpha = .32f),
    )
}
