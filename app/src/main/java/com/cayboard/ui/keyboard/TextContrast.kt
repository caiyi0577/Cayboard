package com.cayboard.ui.keyboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

internal fun readableThemeText(color: Color, background: Color): Color {
    val opaque = color.copy(alpha = 1f)
    val endpoint = if (textContrast(Color.Black, background) >= textContrast(Color.White, background))
        Color.Black else Color.White
    // Already-readable theme colors are untouched; adjust only colors that cannot work
    // on their own nominal theme surface.
    if (textContrast(opaque, background) >= 4.5f) return opaque
    val target = minOf(4.8f, textContrast(endpoint, background))
    var low = 0
    var high = 255
    while (low < high) {
        val middle = (low + high) / 2
        if (textContrast(lerp(opaque, endpoint, middle / 255f), background) >= target)
            high = middle else low = middle + 1
    }
    return lerp(opaque, endpoint, low / 255f)
}

internal fun textContrast(first: Color, second: Color): Float {
    val firstLuminance = first.luminance()
    val secondLuminance = second.luminance()
    return (maxOf(firstLuminance, secondLuminance) + 0.05f) /
        (minOf(firstLuminance, secondLuminance) + 0.05f)
}
