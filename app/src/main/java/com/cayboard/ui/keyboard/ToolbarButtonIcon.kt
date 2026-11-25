package com.cayboard.ui.keyboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.cayboard.R
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
    val iconTint = tint.copy(alpha = tint.alpha * if (active) 0.86f else 0.72f)
    when (item) {
        is ToolbarButtonItem.Builtin -> if (item.button == com.cayboard.keyboard.ToolbarButton.TRADITIONAL) {
            GlyphIcon(if (active) "繁" else "简", iconTint, modifier, contentDescription ?: item.stateLabel(active))
        } else {
            val resource = when (item.button) {
                com.cayboard.keyboard.ToolbarButton.EMOJI -> R.drawable.lucide_smile
                com.cayboard.keyboard.ToolbarButton.CLIPBOARD -> R.drawable.lucide_clipboard
                com.cayboard.keyboard.ToolbarButton.SCHEMA -> R.drawable.lucide_keyboard
                com.cayboard.keyboard.ToolbarButton.TRANSLATE -> R.drawable.lucide_languages
                com.cayboard.keyboard.ToolbarButton.VOICE -> R.drawable.lucide_mic
                com.cayboard.keyboard.ToolbarButton.SETTINGS -> R.drawable.lucide_settings
                com.cayboard.keyboard.ToolbarButton.FLOAT -> R.drawable.lucide_square_dimensions
                com.cayboard.keyboard.ToolbarButton.HANDWRITING_LOOKUP -> R.drawable.lucide_pen_line
                com.cayboard.keyboard.ToolbarButton.SYMBOL -> R.drawable.lucide_omega
                com.cayboard.keyboard.ToolbarButton.SOUND -> R.drawable.lucide_volume_2
                com.cayboard.keyboard.ToolbarButton.FLOATING -> R.drawable.lucide_picture_in_picture_2
                com.cayboard.keyboard.ToolbarButton.ARRANGE -> R.drawable.lucide_grid_2x2
                com.cayboard.keyboard.ToolbarButton.EDIT -> R.drawable.lucide_edit_box
                com.cayboard.keyboard.ToolbarButton.FOURTEEN_SWIPE -> R.drawable.lucide_swipe
                com.cayboard.keyboard.ToolbarButton.VIBRATION -> R.drawable.lucide_activity
                com.cayboard.keyboard.ToolbarButton.FULL_SHAPE ->
                    if (active) R.drawable.lucide_full_circle else R.drawable.lucide_half_circle
                com.cayboard.keyboard.ToolbarButton.TRADITIONAL -> R.drawable.lucide_languages
                else -> null
            }
            Box(
                modifier = modifier.semantics {
                    this.contentDescription = contentDescription ?: item.stateLabel(active)
                },
                contentAlignment = Alignment.Center,
            ) {
                if (resource != null) {
                    Icon(
                        painter = painterResource(resource),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(21.dp),
                    )
                } else {
                    Icon(
                        imageVector = if (item.button == com.cayboard.keyboard.ToolbarButton.FULL_SHAPE && active)
                            com.cayboard.keyboard.FullWidthIcon else item.button.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
    }
}

/** Shared linear delete icon for every keyboard layout. */
@Composable
internal fun deleteIconPainter(): Painter = painterResource(R.drawable.lucide_delete_backspace)

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






