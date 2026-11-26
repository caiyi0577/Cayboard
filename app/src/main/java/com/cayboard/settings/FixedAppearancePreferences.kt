package com.cayboard.settings

import android.content.Context
import android.content.SharedPreferences
import com.cayboard.ui.theme.FixedKeyboardStyle

/** Upgrade to the fixed appearance once, without resetting language, dictionaries or input behavior. */
object FixedAppearancePreferences {
    const val MIGRATED = "fixed_keyboard_appearance_v1"
    private val retiredPrefixes = listOf("frosted_glass_", "custom_theme_image_", "keyboard_background_motion",
        "key_gap_light_", "cayboard_icon_")
    private val resetKeys = setOf("keyboard_theme", "dark_mode", "cayboard_visual_style", "cayboard_key_shape",
        SettingsPreferences.KEY_VISUAL_STYLE, SettingsPreferences.KEY_KEY_SHAPE,
        "glass_liquid", "glass_soft_top_edge", "rounded_keyboard_top", "rounded_keyboard_bottom",
        "key_glow_enabled", "key_animation_enabled", "key_text_scale", "corner_hint_opacity",
        "hide_function_key_hints", "uppercase_key_labels", "chinese_uppercase_key_labels",
        "double_pinyin_key_hints", "double_pinyin_key_hints_with_corners",
        "swipe_up_hints_enabled", "swipe_down_hints_enabled", "number_row",
        "split_keyboard_enabled", "split_keyboard_right_g", "wide_keys", "even_merged_keys",
        "split_gap_fraction_portrait", "split_gap_fraction_landscape")

    fun migrate(context: Context) = migrate(SettingsPreferences.getPrefsPublic(context))
    internal fun migrate(prefs: SharedPreferences) {
        if (prefs.getBoolean(MIGRATED, false)) return
        val editor = prefs.edit()
        prefs.all.keys.filter { it in resetKeys || retiredPrefixes.any(it::startsWith) }.forEach(editor::remove)
        editor.putString("keyboard_theme", FixedKeyboardStyle.ID).putInt("dark_mode", 2)
            .putBoolean(MIGRATED, true).commit()
    }
}
