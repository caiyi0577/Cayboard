package com.cayboard.settings

import android.content.Context

data class HardwareKeyboardOptions(
    val ctrlSpace: Boolean = true,
    val shiftTap: Boolean = true,
    val followCursor: Boolean = true,
    val avoidEditor: Boolean = true,
    val dockAtEdge: Boolean = true,
    val pageMinusEquals: Boolean = true,
    val pageBrackets: Boolean = true,
    val pageCommaPeriod: Boolean = true,
    /** 联想词也显示在跟随光标的候选浮窗里（和打字候选同一个位置）；关闭时联想在屏幕底部。默认关。 */
    val associationInWindow: Boolean = false,
    /** 游戏直通：游戏类应用里不显示工具条，实体按键直接交给游戏。默认关。 */
    val gamePassthrough: Boolean = false,
    /** 工具快捷键（Ctrl+. / Ctrl+回车 / Alt+V / Alt+H）。关掉后这些组合键交给应用。默认开。 */
    val toolShortcuts: Boolean = true,
)

object HardwareKeyboardPreferences {
    const val PREFIX = "hardware_keyboard_"
    fun read(context: Context): HardwareKeyboardOptions {
        val p = SettingsPreferences.getPrefsPublic(context)
        return HardwareKeyboardOptions(
            ctrlSpace = p.getBoolean(PREFIX + "ctrl_space", true),
            shiftTap = p.getBoolean(PREFIX + "shift_tap", true),
            followCursor = p.getBoolean(PREFIX + "follow_cursor", true),
            avoidEditor = p.getBoolean(PREFIX + "avoid_editor", true),
            // Fixed product behaviour; an old saved false must not restore the retired toolbar.
            dockAtEdge = true,
            pageMinusEquals = p.getBoolean(PREFIX + "page_minus_equals", true),
            pageBrackets = p.getBoolean(PREFIX + "page_brackets", true),
            pageCommaPeriod = p.getBoolean(PREFIX + "page_comma_period", true),
            associationInWindow = p.getBoolean(PREFIX + "association_in_window", false),
            gamePassthrough = p.getBoolean(PREFIX + "game_passthrough", false),
            toolShortcuts = p.getBoolean(PREFIX + "tool_shortcuts", true),
        )
    }
    fun set(context: Context, key: String, enabled: Boolean) {
        require(key in setOf("ctrl_space", "shift_tap", "follow_cursor", "avoid_editor",
            "page_minus_equals", "page_brackets", "page_comma_period", "association_in_window",
            "game_passthrough", "tool_shortcuts"))
        SettingsPreferences.getPrefsPublic(context).edit().putBoolean(PREFIX + key, enabled).apply()
    }
}
