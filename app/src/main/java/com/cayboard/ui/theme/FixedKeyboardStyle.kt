package com.cayboard.ui.theme

import androidx.compose.ui.graphics.Color

/** The sole keyboard appearance. Colors and keycap states come from the supplied BDS skin. */
object FixedKeyboardStyle {
    const val ID = "gradient_flat"

    val scheme = KeyboardColorScheme(
        id = ID, name = "渐变扁平",
        specialKeyLight = Color(0xFFD1D7DF), specialKeyDark = Color(0xFF798C94),
        accentLight = Color(0xFF356B97), accentDark = Color(0xFFB8D5E0),
        primaryLight = Color(0xFF356B97), primaryDark = Color(0xFFB8D5E0),
        primaryContainerLight = Color(0xFFDCE7F2), primaryContainerDark = Color(0xFF353E42),
        surfaceLight = Color(0xFFF7F7F7), surfaceDark = Color(0xFF1E1E1E),
        keyboardBgLight = Color(0xFFE4E7EC), keyboardBgDark = Color(0xFF1E1E1E),
        keyBgLight = Color(0xFFFFFFFF), keyBgDark = Color(0xFF353E42),
        candidateBarBgLight = Color(0xFFE4E7EC), candidateBarBgDark = Color(0xFF1E1E1E),
        keyTextColorLight = Color(0xFF26313B), keyTextColorDark = Color(0xFFEDEEEF),
        candidateTextColorLight = Color(0xFF414550), candidateTextColorDark = Color(0xFFE1E3E5),
        candidateSelectedTextColorLight = Color(0xFF356B97), candidateSelectedTextColorDark = Color(0xFFB8D5E0),
        dividerColorLight = Color(0xFFDDE1E4), dividerColorDark = Color(0xFF353E42),
        specialKeyTextColorLight = Color(0xFF26313B), specialKeyTextColorDark = Color(0xFFE1E3E5),
        useThemeColors = true,
    )

    /** Light appearance uses one white typing surface, with a separate gray control surface. */
    fun lightKeyFill(label: String, pressed: Boolean): Color = when {
        pressed -> Color(0xFFCBD8E6)
        rowFor(label) == 3 || label in setOf("删除", "退格") -> scheme.specialKeyLight
        else -> scheme.keyBgLight
    }

    /** Key rows in both the 26-key and 9-key layouts; controls use the fourth-row keycap. */
    fun rowFor(label: String): Int {
        val letters = label.lowercase().filter { it in 'a'..'z' }
        if (label in setOf("分词", "1", "2", "3") || letters in setOf("abc", "def")) return 0
        if (label in setOf("4", "5", "6") || letters in setOf("ghi", "jkl", "mno")) return 1
        if (label in setOf("7", "8", "9", "删除", "退格") || letters in setOf("pqrs", "tuv", "wxyz")) return 2
        if (letters.isNotEmpty() && letters.length <= 2) {
            if (letters.all { it in "qwertyuiop" }) return 0
            if (letters.all { it in "asdfghjkl" }) return 1
            if (letters.all { it in "zxcvbnm" }) return 2
        }
        return 3
    }
}
