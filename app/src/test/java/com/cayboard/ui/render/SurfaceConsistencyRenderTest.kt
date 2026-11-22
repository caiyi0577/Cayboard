package com.cayboard.ui.render

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.settings.KeysConfigHelper
import com.cayboard.ui.keyboard.CursorControlOverlay
import com.cayboard.ui.keyboard.KaomojiKeyboard
import com.cayboard.ui.keyboard.KeyboardCallbacks
import com.cayboard.ui.keyboard.KeyboardInputPreferences
import com.cayboard.ui.keyboard.KeyboardLayout
import com.cayboard.ui.keyboard.LocalKeyboardExplicitWidth
import com.cayboard.ui.keyboard.LocalKeyboardInputPreferences
import com.cayboard.ui.keyboard.hiddenDuringCursorControl
import com.cayboard.ui.theme.KeyboardThemes
import com.cayboard.ui.theme.CayboardTheme
import com.cayboard.ui.theme.resolveKeyboardPalette
import com.cayboard.viewmodel.KeyboardUiState
import com.cayboard.viewmodel.KeyboardViewModel
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 一致性修复的出图核对：光标调整遮罩（盖在 26 键上）、颜文字空页，
 * 柔和蓝 / 黑白 / iOS 渐变 / 薰衣草紫 × 深浅色 × 毛玻璃。只在设了 CYIME_RENDER_OUT 时出图。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w1300dp-h400dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SurfaceConsistencyRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun renderOverlaysAcrossThemes() {
        val out = System.getenv("CYIME_RENDER_OUT")
        assumeTrue(!out.isNullOrEmpty())
        val app = ApplicationProvider.getApplicationContext<Application>()
        KeysConfigHelper.loadConfig(app); KeyboardThemes.reload(app)
        com.cayboard.ui.keyboard.AppFonts.initialize(app)
        val vm = KeyboardViewModel(app)
        var theme by mutableStateOf("soft_blue")
        var dark by mutableStateOf(false)
        var glass by mutableStateOf(false)
        rule.setContent {
            val palette = resolveKeyboardPalette(theme, dark)
            val config = Unit
            CayboardTheme(darkTheme = dark, themeId = theme) {
                CompositionLocalProvider(LocalKeyboardExplicitWidth provides true,
                    LocalKeyboardInputPreferences provides KeyboardInputPreferences()) {
                    Row(Modifier.requiredSize(1240.dp, 300.dp).testTag("surface-body")
                        // 玻璃时后面是一张彩色页面，看透出去的效果。
                        .then(if (glass) Modifier.background(Brush.linearGradient(listOf(Color(0xFF2E7D32),
                            Color(0xFFF9A825), Color(0xFFC62828), Color(0xFF1565C0)))) else Modifier)) {
                        val keyboardBg = Modifier.background(palette.background)
                        Box(Modifier.requiredSize(480.dp, 300.dp).then(keyboardBg)) {
                            KeyboardLayout({}, vm, KeyboardCallbacks(onKeyPress = { _, _ -> }, onCandidateSelect = {}),
                                KeyboardUiState(currentSchemaId = "rime_ice", themeId = theme, isDarkTheme = dark),
                                false, Modifier.requiredSize(480.dp, 300.dp).hiddenDuringCursorControl(true))
                            CursorControlOverlay(palette.background, palette.text, Modifier.requiredSize(480.dp, 300.dp))
                        }
                        Box(Modifier.requiredSize(380.dp, 300.dp).then(keyboardBg)) {
                            KaomojiKeyboard({}, Modifier.requiredSize(380.dp, 300.dp), palette.text, palette.accent)
                        }
                    }
                }
            }
        }
        // 颜文字停在「最近」页：新装没有记录，显示空页提示。
        rule.onNodeWithTag("kaomoji-recent", true).performClick()
        File(out!!).mkdirs()
        fun shot(name: String) {
            val body = rule.onNodeWithTag("surface-body", true).fetchSemanticsNode().boundsInRoot
            val root = rule.activity.window.decorView
            val full = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            rule.runOnIdle { root.draw(android.graphics.Canvas(full)) }
            val crop = Bitmap.createBitmap(full, body.left.toInt().coerceAtLeast(0), body.top.toInt().coerceAtLeast(0),
                body.width.toInt().coerceAtMost(full.width), body.height.toInt().coerceAtMost(full.height))
            File(out, "$name.png").outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        for (t in listOf("soft_blue", "pure_black", "transparent_glass", "lavender_purple")) for (d in listOf(false, true)) for (g in listOf(false, true)) {
            KeysConfigHelper.setActiveKeyboardSchema("rime_ice")
            // 面板底色按设置里的主题取渐变/图片（KeyboardPanelBackground），不设的话 iOS 渐变出图成纯黑。
            com.cayboard.settings.SettingsPreferences.getPrefsPublic(app).edit().putString("keyboard_theme", t).commit()
            rule.runOnIdle { vm.resetKeyboard(false, "rime_ice"); theme = t; dark = d; glass = g }
            rule.waitForIdle()
            shot("$t-${if (d) "dark" else "light"}-${if (g) "glass" else "solid"}")
        }
    }
}
