package com.cayboard.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.cayboard.settings.KeysConfigHelper

/** Resolved keyboard colors, shared by the keyboard, material samples and preview export. */
@Immutable
data class KeyboardPalette(
    val background: Color, val key: Color, val text: Color, val accent: Color,
    val function: Color, val functionText: Color, val enter: Color,
    val candidateText: Color, val selectedText: Color,
    /**
     * 眼睛看到的键盘底色：纯色主题就是 [background]；渐变取各色的平均。iOS 渐变深色的 [background] 是 #000，
     * 键盘上看到的却是深灰渐变，盖在键盘上的实心面板用这个才和键盘底一致。图片主题没有可取的颜色，仍是 [background]。
     */
    val surface: Color = background,
)

fun resolveKeyboardPalette(themeId: String, dark: Boolean): KeyboardPalette {
    val s = FixedKeyboardStyle.scheme
    return KeyboardPalette(
        background = if (dark) s.keyboardBgDark else s.keyboardBgLight,
        key = if (dark) s.keyBgDark else s.keyBgLight,
        text = if (dark) s.keyTextColorDark else s.keyTextColorLight,
        accent = if (dark) s.accentDark else s.accentLight,
        function = if (dark) s.specialKeyDark else s.specialKeyLight,
        functionText = if (dark) s.specialKeyTextColorDark!! else s.specialKeyTextColorLight!!,
        enter = if (dark) s.specialKeyDark else s.specialKeyLight,
        candidateText = if (dark) s.candidateTextColorDark else s.candidateTextColorLight,
        selectedText = if (dark) s.candidateSelectedTextColorDark else s.candidateSelectedTextColorLight,
    )
}

internal val LocalKeyboardPalette = staticCompositionLocalOf<KeyboardPalette?> { null }

fun resolvedSpecialKeyColor(theme: KeyboardColorScheme, isDark: Boolean, legacyOverride: Color?): Color =
    if (isDark) theme.specialKeyDark else theme.specialKeyLight
