package com.cayboard.ui.keyboard

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** CJK punctuation occupies a full cell but its ink often sits in a corner of that cell. */
internal fun isKeyboardGlyphLabel(text: String): Boolean = text.isNotBlank() && text.codePoints().allMatch {
    Character.getType(it) in setOf(
        Character.CONNECTOR_PUNCTUATION.toInt(), Character.DASH_PUNCTUATION.toInt(),
        Character.START_PUNCTUATION.toInt(), Character.END_PUNCTUATION.toInt(),
        Character.INITIAL_QUOTE_PUNCTUATION.toInt(), Character.FINAL_QUOTE_PUNCTUATION.toInt(),
        Character.OTHER_PUNCTUATION.toInt(), Character.MATH_SYMBOL.toInt(),
        Character.CURRENCY_SYMBOL.toInt(), Character.MODIFIER_SYMBOL.toInt(), Character.OTHER_SYMBOL.toInt(),
    ) || Character.isWhitespace(it)
}

/** Keep real text/semantics and input unchanged; center only punctuation's painted glyph bounds. */
@Composable
internal fun KeyboardGlyphText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = AppFonts.keyFontFamily,
    fontWeight: FontWeight = FontWeight.Normal,
    textDecoration: TextDecoration? = null,
) {
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val glyph = remember(text) { isKeyboardGlyphLabel(text) }
    val typeface = LocalFontFamilyResolver.current.resolve(fontFamily, fontWeight).value as Typeface
    val pixels = with(LocalDensity.current) { fontSize.toPx() }
    val ink = remember(text, typeface, pixels) {
        Rect().also { rect ->
            if (glyph) Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = pixels
            }.getTextBounds(text, 0, text.length, rect)
        }
    }
    Text(
        text = text, fontSize = fontSize, color = color, fontFamily = fontFamily,
        fontWeight = fontWeight, textAlign = TextAlign.Center, maxLines = 1,
        letterSpacing = 0.sp, softWrap = false, lineHeight = fontSize * 1.2f, textDecoration = textDecoration,
        onTextLayout = { layout = it },
        modifier = modifier.drawWithContent {
            val result = layout
            if (!glyph || ink.isEmpty || result == null || result.lineCount != 1) drawContent()
            else translate(
                left = (size.width - ink.width()) / 2f - result.getLineLeft(0) - ink.left,
                top = (size.height - ink.height()) / 2f - result.firstBaseline - ink.top,
            ) { this@drawWithContent.drawContent() }
        },
    )
}
