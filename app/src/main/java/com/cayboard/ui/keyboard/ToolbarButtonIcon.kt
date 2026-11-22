package com.cayboard.ui.keyboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cayboard.keyboard.ToolbarButtonItem

/** Built-in toolbar icons share the same size and baseline. */
@Composable
fun ToolbarButtonIcon(
    item: ToolbarButtonItem,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    fontSize: TextUnit = 16.sp,
    active: Boolean = false,
) {
    when (item) {
        is ToolbarButtonItem.Builtin -> if (item.button == com.cayboard.keyboard.ToolbarButton.TRADITIONAL) {
            GlyphIcon(if (active) "繁" else "简", tint, modifier, contentDescription ?: item.stateLabel(active))
        } else Icon(
            imageVector = if (item.button == com.cayboard.keyboard.ToolbarButton.FULL_SHAPE && active)
                com.cayboard.keyboard.FullWidthIcon else item.button.icon,
            contentDescription = contentDescription ?: item.stateLabel(active),
            tint = tint,
            modifier = modifier,
        )
    }
}

/** 用一个字当图标：字高按图标框算（框的 0.78），和旁边的线条图标看起来一样大。 */
@Composable
private fun GlyphIcon(text: String, tint: Color, modifier: Modifier, description: String) {
    BoxWithConstraints(
        modifier = modifier.semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val box = minOf(maxWidth, maxHeight).let { if (it == Dp.Infinity) 24.dp else it }
        val fontSize = with(LocalDensity.current) { (box * 0.78f).toSp() }
        Text(
            text = text,
            color = tint,
            maxLines = 1,
            style = TextStyle(
                fontSize = fontSize,
                lineHeight = fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** 键盘上显示的名称：全角/半角、简繁开关按当前状态显示，其余按钮用原名称。 */
internal fun ToolbarButtonItem.stateLabel(active: Boolean): String = when {
    this is ToolbarButtonItem.Builtin && button == com.cayboard.keyboard.ToolbarButton.FULL_SHAPE ->
        if (active) "全角" else "半角"
    this is ToolbarButtonItem.Builtin && button == com.cayboard.keyboard.ToolbarButton.TRADITIONAL ->
        if (active) "繁体" else "简体"
    else -> label
}
