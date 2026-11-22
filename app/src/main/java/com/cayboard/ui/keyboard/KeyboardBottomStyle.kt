package com.cayboard.ui.keyboard

import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.cayboard.settings.SettingsPreferences

/** Shared persisted choice; independent of keyboard dimensions and insets. */
@Composable
internal fun rememberRoundedKeyboardBottom(): Boolean =
    rememberBooleanChoice(SettingsPreferences.KEY_ROUNDED_KEYBOARD_BOTTOM, SettingsPreferences::roundedKeyboardBottom)

/** 固定键盘顶部两角是否圆角，和底部圆角同一种读法。 */
@Composable
internal fun rememberRoundedKeyboardTop(): Boolean =
    rememberBooleanChoice(SettingsPreferences.KEY_ROUNDED_KEYBOARD_TOP, SettingsPreferences::roundedKeyboardTop)

@Composable
internal fun rememberInlineQuickActionEditing(): Boolean =
    rememberBooleanChoice(SettingsPreferences.KEY_INLINE_QUICK_ACTION_EDITING, SettingsPreferences::inlineQuickActionEditing)

@Composable
internal fun rememberFloatingGeometryLocked(): Boolean =
    rememberBooleanChoice(SettingsPreferences.KEY_FLOATING_GEOMETRY_LOCKED, SettingsPreferences::floatingGeometryLocked)

@Composable
private fun rememberBooleanChoice(prefKey: String, read: (android.content.Context) -> Boolean): Boolean {
    val context = LocalContext.current
    var value by remember(context, prefKey) { mutableStateOf(read(context)) }
    DisposableEffect(context, prefKey) {
        val prefs = SettingsPreferences.getPrefsPublic(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == prefKey) {
                value = read(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return value
}

internal fun useDarkNavigationIcons(rounded: Boolean, systemDark: Boolean, keyTextLuminance: Float): Boolean =
    if (rounded) !systemDark else keyTextLuminance < 0.5f

