package com.cayboard.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class FeedbackShortcutSettingsTest {
    @Test fun oldMenuGetsVibrationOnceAndKeepsLaterRemoval() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SettingsPreferences.getPrefsPublic(context)
        prefs.edit().clear().putString("toolbar_buttons", "schema,clipboard")
            .putString("menu_buttons", "settings,sound,arrange").apply()
        SettingsPreferences.setSoundEnabled(context, false)
        SettingsPreferences.setVibrationEnabled(context, true)
        val menu = SettingsPreferences.getMenuButtons(context)
        assertEquals(menu.indexOf("sound") + 1, menu.indexOf("vibration"))
        assertEquals(1, menu.count { it == "vibration" })
        assertFalse(SettingsPreferences.isSoundEnabled(context))
        assertTrue(SettingsPreferences.isVibrationEnabled(context))
        prefs.edit().putString("menu_buttons", menu.filterNot { it == "vibration" }.joinToString(",")).apply()
        assertFalse("Respect later customization", "vibration" in SettingsPreferences.getMenuButtons(context))
    }
}
