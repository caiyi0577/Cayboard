package com.cayboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Fixed selection colors; controls choose strong fill for small tokens or quiet fill for larger surfaces. */
internal data class SelectionColors(val fill: Color, val content: Color, val quietFill: Color, val quietContent: Color)
internal object SelectionStyle {
    private val light = SelectionColors(Color(0xFF356B97), Color.White, Color(0xFFE0EBF3), Color(0xFF285B83))
    private val dark = SelectionColors(Color(0xFF416879), Color(0xFFF7FAFC), Color(0xFF2C414D), Color(0xFFD8EAF3))
    fun colors(dark: Boolean): SelectionColors = if (dark) this.dark else light
}
@Composable
internal fun selectionColors(): SelectionColors = SelectionStyle.colors(MaterialTheme.colorScheme.surface.luminance() < .5f)
