package com.cayboard.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.cayboard.settings.BackgroundConfig
import com.cayboard.settings.ColorSchemeEntry
import com.cayboard.settings.KeysConfigHelper

data class KeyboardColorScheme(
    val id: String,
    val name: String,
    val specialKeyLight: Color,
    val specialKeyDark: Color,
    val accentLight: Color,
    val accentDark: Color,
    val primaryLight: Color = accentLight,
    val primaryDark: Color = accentDark,
    val primaryContainerLight: Color = specialKeyLight,
    val primaryContainerDark: Color = specialKeyDark,
    val surfaceLight: Color = Color.White,
    val surfaceDark: Color = Color(0xFF1C1B1F),
    // 键盘背景色
    val keyboardBgLight: Color = surfaceLight,
    val keyboardBgDark: Color = surfaceDark,
    // 按键颜色
    val keyBgLight: Color = Color.White,
    val keyBgDark: Color = Color(0xFF4A4A4A),
    // 候选栏颜色
    val candidateBarBgLight: Color = surfaceLight,
    val candidateBarBgDark: Color = surfaceDark,
    // 按键文字颜色
    val keyTextColorLight: Color = Color(0xFF202124),
    val keyTextColorDark: Color = Color(0xFFE8EAED),
    // 候选文字颜色
    val candidateTextColorLight: Color = Color(0xFF1A73E8),
    val candidateTextColorDark: Color = Color(0xFF8AB4F8),
    // 候选选中文字颜色（未配置时回退到主题强调色）
    val candidateSelectedTextColorLight: Color = Color(0xFF8F73E2),
    val candidateSelectedTextColorDark: Color = Color(0xFFD0BCFF),
    // 分隔线颜色
    val dividerColorLight: Color = Color(0xFFDADCE0),
    val dividerColorDark: Color = Color(0xFF3C4043),
    // 背景配置（纯色/渐变/图片），比上方 flat color 字段优先级更高
    val keyboardBackground: BackgroundConfig? = null,
    val keyBackground: BackgroundConfig? = null,
    val candidateBarBackground: BackgroundConfig? = null,
    /** Material You 动态配色：颜色来自壁纸调色板，随壁纸变化。 */
    val isDynamic: Boolean = false,
    /** 内置完整调色板优先于旧全局颜色；固定主题与动态色兼容回退同样适用。 */
    val useThemeColors: Boolean = false,
    val enterKeyLight: Color? = null,
    val enterKeyDark: Color? = null,
    val specialKeyTextColorLight: Color? = null,
    val specialKeyTextColorDark: Color? = null,
)

/** Compatibility facade for keyboard components; there is no theme catalogue or imported color configuration. */
object KeyboardThemes {
    val themes: List<KeyboardColorScheme> get() = listOf(FixedKeyboardStyle.scheme)
    fun initFromConfig(context: Context) = reload(context)
    fun reload(context: Context) { VisualStyles.current = VisualStyle.ORIGINAL; KeyShapes.current = KeyShape.SQUARE }
    fun refreshDynamicSchemes(context: Context): Boolean = false
    fun getThemeById(id: String): KeyboardColorScheme = FixedKeyboardStyle.scheme
    fun getRenderingScheme(id: String): KeyboardColorScheme = FixedKeyboardStyle.scheme
    fun getSpecialKeyColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.specialKeyDark else FixedKeyboardStyle.scheme.specialKeyLight
    fun getAccentColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.accentDark else FixedKeyboardStyle.scheme.accentLight
    fun getEnterKeyColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.specialKeyDark else FixedKeyboardStyle.scheme.specialKeyLight
    fun getSpecialKeyTextColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.specialKeyTextColorDark!! else FixedKeyboardStyle.scheme.specialKeyTextColorLight!!
    fun getPrimaryColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.primaryDark else FixedKeyboardStyle.scheme.primaryLight
    fun getPrimaryContainerColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.primaryContainerDark else FixedKeyboardStyle.scheme.primaryContainerLight
    fun getSurfaceColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.surfaceDark else FixedKeyboardStyle.scheme.surfaceLight
    fun getKeyboardBackgroundColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.keyboardBgDark else FixedKeyboardStyle.scheme.keyboardBgLight
    fun getKeyBackgroundColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.keyBgDark else FixedKeyboardStyle.scheme.keyBgLight
    fun getCandidateBarBackgroundColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.candidateBarBgDark else FixedKeyboardStyle.scheme.candidateBarBgLight
    fun getKeyTextColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.keyTextColorDark else FixedKeyboardStyle.scheme.keyTextColorLight
    fun getCandidateTextColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.candidateTextColorDark else FixedKeyboardStyle.scheme.candidateTextColorLight
    fun getCandidateSelectedTextColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.candidateSelectedTextColorDark else FixedKeyboardStyle.scheme.candidateSelectedTextColorLight
    fun getDividerColor(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.dividerColorDark else FixedKeyboardStyle.scheme.dividerColorLight
    fun getKeyBgColorOverride(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.keyBgDark else FixedKeyboardStyle.scheme.keyBgLight
    fun getKeyTextColorOverride(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.keyTextColorDark else FixedKeyboardStyle.scheme.keyTextColorLight
    fun getCandidateTextColorOverride(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.candidateTextColorDark else FixedKeyboardStyle.scheme.candidateTextColorLight
    fun getCandidateSelectedTextColorOverride(themeId: String, isDark: Boolean): Color = if (isDark) FixedKeyboardStyle.scheme.candidateSelectedTextColorDark else FixedKeyboardStyle.scheme.candidateSelectedTextColorLight
}
