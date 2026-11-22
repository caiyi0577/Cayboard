package com.cayboard.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.cayboard.keyboard.ToolbarAction
import com.cayboard.keyboard.ToolbarButton
import com.cayboard.keyboard.ToolbarButtonItem
import com.cayboard.settings.SettingsPreferences
import com.cayboard.ui.keyboard.KeyboardInputPreferences
import com.cayboard.ui.keyboard.MenuBar
import com.cayboard.ui.keyboard.MenuBarState
import com.cayboard.ui.keyboard.flipGesture
import com.cayboard.ui.keyboard.gestureOn
import com.cayboard.ui.keyboard.rememberKeyboardInputPreferences
import com.cayboard.ui.settings.SettingsMainContent
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 九键菜单里能直接开关拂动选字母；设置首页搜“长按”能找到它并跳到所在页。 */
class GestureMenuAndSearchTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun searchFindsFlickAndOpensItsPage() {
        var opened: String? = null
        rule.setContent {
            MaterialTheme {
                SettingsMainContent(onNavigateToSchema = {}, onNavigateToTheme = {}, onNavigateToKeyEffect = {},
                    onNavigateToLayoutDisplay = {},  onNavigateToPlugins = {},
                    onNavigateToSmartPrediction = {}, onNavigateToSpeechToText = {}, onNavigateToAbout = {},
                    onNavigateToRoute = { opened = it })
            }
        }
        rule.onNodeWithTag("settings-search").performTextInput("长按")
        rule.onNodeWithTag("settings-search-result:拂动选字母").assertIsDisplayed()
        save("search-long-press")
        rule.onNodeWithTag("settings-search-result:拂动选字母").performClick()
        rule.runOnIdle { assertEquals("layout_input_nine", opened) }
    }

    private fun save(name: String) {
        rule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "gesture-menu-search").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        image.recycle()
    }
}
