package com.cayboard.ui.keyboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.cayboard.ui.theme.KeyboardPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageMenuColorsTest {
    private fun palette(background: Long, key: Long, function: Long, text: Long, accent: Long) = KeyboardPalette(
        background = Color(background), key = Color(key), text = Color(text), accent = Color(accent),
        function = Color(function), functionText = Color(text), enter = Color(function),
        candidateText = Color(text), selectedText = Color(text))

    private val palettes = listOf(
        palette(0xFF000000, 0xFF1C1C1E, 0xFF29292B, 0xFFFFFFFF, 0xFFE0E0E0), // 黑白·深
        palette(0xFFE8E8EA, 0xFFFFFFFF, 0xFFD4D4D8, 0xFF202020, 0xFF454545), // 黑白·浅
        palette(0xFF33333A, 0xFF55555A, 0xFF3A3A3C, 0xFFFFFFFF, 0xFFE0E0E0), // iOS 渐变·深
        palette(0xFFF5F0FA, 0xFFFFFFFF, 0xFFDCE6FA, 0xFFB0B0B0, 0xFF42639C), // 浅色主题但文字太浅
    )
    private val glassConfigs = listOf(Unit,
        Unit, Unit)

    

    

    @Test fun `menu keeps the theme surface instead of falling back to the material surface`() {
        val p = palettes[3]
        val material = Color(0xFF6750A4)
        val colors = languageMenuColors(p, material, Color.Black, Color.Blue)
        assertTrue(colors.surface != material)
        assertTrue(textContrast(colors.text, colors.surface) >= 4.5f)
    }

    
}
