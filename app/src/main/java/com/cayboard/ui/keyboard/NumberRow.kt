package com.cayboard.ui.keyboard

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp

/**
 * 「数字行」（设置 → 键盘布局 → 数字键盘，默认开）：26 键、14 键字母上方加一排，键和顺序在「按键符号 → 数字行」里改。
 * 键盘总高度不变，数字行比字母行矮一点（字母行的 [NUMBER_ROW_HEIGHT_WEIGHT]），字母键少缩一些。
 */
internal const val NUMBER_ROW_HEIGHT_WEIGHT = 0.8f

/** 分体中缝按行登记位置（[SplitGapSpacer]）；字母行用 0-3，数字行用一个不会撞上的行号。 */
internal const val NUMBER_ROW_SPLIT_ROW = 9

/** 这张字母键盘要不要数字行：注音（大千）首排本身就是数字位，不再加。 */
internal fun numberRowShown(setting: Boolean, zhuyin: Boolean): Boolean = setting && !zhuyin

/** 分体时左半的键数：一半归左手，单数时多出的一个给右手；不分体或只有一个键时 -1。 */
internal fun numberRowSplitAt(count: Int, split: Boolean): Int = if (split && count >= 2) count / 2 else -1

/**
 * 每个键的份数：不分体时平分整行；分体时左右两半各占一半宽，中缝和字母行的中缝对齐，
 * 键数是单数也不会把缝挤偏。
 */
internal fun numberRowSpans(count: Int, splitAt: Int): List<Float> =
    if (splitAt !in 1 until count) List(count) { 1f }
    else List(count) { index -> if (index < splitAt) 1f / splitAt else 1f / (count - splitAt) }

/** 电脑键盘上数字键按 Shift 出的符号；中英文都一样，不跟全角/半角。 */
private val NUMBER_SHIFT_SYMBOLS = "1234567890".zip("!@#$%^&*()").associate { (digit, symbol) -> digit.toString() to symbol.toString() }

/**
 * 数字行键的角标：和电脑键帽一样，1 上是 !，……，0 上是 )。跟着数字走，「按键符号 → 数字行」里改了顺序也不变；
 * 不是数字的格子（自己加的符号、短语）没有角标。开了数字行，这些符号从 26 键第一排挪到这里（用户 11-01 定）。
 */
internal fun numberRowShiftSymbol(key: String): String? = NUMBER_SHIFT_SYMBOLS[key]

/**
 * 数字行的一排键：点按把键上写的原样上屏（[onInput]，和字母键上角标的数字同一条路，不拿来选候选）；
 * 长按在字符盘里选择数字或对应的角标符号。隐藏角标只影响键帽文字。
 */
@Composable
internal fun NumberKeyRow(
    keys: List<String>,
    splitAt: Int,
    splitGap: Dp,
    keyBackgroundColor: Color,
    keyTextColor: Color,
    shadowEnabled: Boolean,
    shadowElevation: Dp,
    shadowShapeRadius: Dp,
    onInput: (String) -> Unit,
    onKeyPressDown: ((String) -> Unit)?,
    onKeyRelease: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    showHints: Boolean = true,
) {
    val spans = numberRowSpans(keys.size, splitAt)
    Row(modifier.fillMaxWidth().testTag("number-row")) {
        keys.forEachIndexed { index, key ->
            if (index == splitAt) SplitGapSpacer(NUMBER_ROW_SPLIT_ROW, splitGap)
            val shift = numberRowShiftSymbol(key)
            SwipeableKeyButton(
                text = key,
                onClick = { onInput(key) },
                swipeUpKeyLabel = shift?.takeIf { showHints },
                symbolInputText = shift,
                onLongPressSelect = onInput,
                cancelTapOnDrag = true,
                backgroundColor = keyBackgroundColor,
                textColor = keyTextColor,
                modifier = Modifier.weight(spans[index]).testTag("number-row-key:$index"),
                onPress = { onKeyPressDown?.invoke(key) },
                onRelease = { onKeyRelease?.invoke(key) },
                shadowEnabled = shadowEnabled,
                shadowElevation = shadowElevation,
                shadowShapeRadius = shadowShapeRadius,
            )
        }
    }
}
