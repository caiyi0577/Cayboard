package com.cayboard.ui.render

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.mockito.kotlin.whenever
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.settings.KeysConfigHelper
import com.cayboard.ui.keyboard.*
import com.cayboard.ui.theme.*
import com.cayboard.viewmodel.KeyboardUiState
import com.cayboard.viewmodel.KeyboardViewModel
import com.cayboard.engine.T9InputController
import org.mockito.kotlin.mock
import org.mockito.kotlin.doReturn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Assert.assertEquals
import androidx.compose.ui.graphics.toArgb
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Render actual layouts offline, including light/dark and a pressed keycap sample. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h470dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FixedKeyboardLightRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun renderKeyboardLayouts() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        KeysConfigHelper.loadConfig(app)
        AppFonts.initialize(app)
        KeyboardThemes.reload(app)
        val vm = KeyboardViewModel(app)
        val callbacks = KeyboardCallbacks(onKeyPress = { _, _ -> }, onCandidateSelect = {})
        val controller = mock<T9InputController> {
            on { leftPanelState } doReturn T9InputController.LeftPanelState.IDLE
            on { firstOptions } doReturn emptyList()
        }
        var schema by mutableStateOf("t9_pinyin")
        var dark by mutableStateOf(false)
        rule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            val palette = resolveKeyboardPalette(FixedKeyboardStyle.ID, dark)
            CompositionLocalProvider(LocalConfiguration provides configuration,
                LocalKeyboardExplicitWidth provides true,
                LocalKeyboardPunctuation provides KeyboardPunctuation(full = true, japanese = false, chinese = true),
                LocalKeyboardPalette provides palette,
                LocalKeyboardInputPreferences provides KeyboardInputPreferences()) {
                CayboardTheme(darkTheme = dark) {
                    Column(Modifier.requiredSize(411.dp, 365.dp).background(palette.background).testTag("keyboard")) {
                        CandidateBar(
                            state = CandidateBarState.ChineseCandidates(listOf("你好", "拟好", "你", "妮"), inputText = "nihao", hasMore = true),
                            visuals = CandidateBarVisuals(palette.background, palette.candidateText,
                                KeyboardThemes.getDividerColor(FixedKeyboardStyle.ID, dark), palette.accent,
                                palette.selectedText, dark, palette.key),
                            callbacks = CandidateBarCallbacks(onCandidateSelect = {}, onShowMoreCandidates = {}),
                            showPreeditPreview = false, modifier = Modifier.fillMaxWidth().height(44.dp))
                        val state = KeyboardUiState(currentSchemaId = schema, themeId = FixedKeyboardStyle.ID, isDarkTheme = dark)
                        if (schema == "number") NumberKeyboardLayout({}, palette.key, palette.text,
                            KeyboardThemes.getSpecialKeyColor(FixedKeyboardStyle.ID, dark),
                            keyboardBackgroundColor = palette.background,
                            specialKeyTextColor = KeyboardThemes.getSpecialKeyTextColor(FixedKeyboardStyle.ID, dark),
                            modifier = Modifier.fillMaxWidth().weight(1f))
                        else if (schema == "symbols") SymbolKeyboardLayout({}, {}, backgroundColor = palette.background,
                            textColor = palette.text, accentColor = palette.accent, keyBgColor = palette.key,
                            onNumber = {}, modifier = Modifier.fillMaxWidth().weight(1f),
                            specialKeyBackgroundColor = KeyboardThemes.getSpecialKeyColor(FixedKeyboardStyle.ID, dark),
                            specialKeyTextColor = KeyboardThemes.getSpecialKeyTextColor(FixedKeyboardStyle.ID, dark))
                        else if (schema == "edit") EditKeyboardLayout({}, {}, palette.background,
                            palette.text, palette.accent, palette.key, shadowElevation = 20.dp,
                            modifier = Modifier.fillMaxWidth().weight(1f))
                        else if (schema == "t9_pinyin") T9KeyboardLayout({}, callbacks, state, controller,
                            palette.key, palette.text, KeyboardThemes.getSpecialKeyColor(FixedKeyboardStyle.ID, dark),
                            accentColor = palette.accent, keyboardBackgroundColor = palette.background,
                            specialKeyTextColor = KeyboardThemes.getSpecialKeyTextColor(FixedKeyboardStyle.ID, dark),
                            modifier = Modifier.fillMaxWidth().weight(1f))
                        else KeyboardLayout({}, vm, callbacks, state, false, Modifier.fillMaxWidth().weight(1f))
                        Row(Modifier.fillMaxWidth().height(42.dp).testTag("pressed")) {
                            Box(Modifier.weight(1f).fillMaxHeight().padding(1.dp).fixedSkinKeycap("abc", true))
                            Box(Modifier.weight(1f).fillMaxHeight().padding(1.dp).fixedSkinKeycap("功能", true))
                        }
                    }
                }
            }
        }
        val out = File(System.getenv("CAYBOARD_RENDER_OUT") ?: "build/fixed-keyboard-renders").apply { mkdirs() }
        val sharedAtlas = FixedKeycapAtlas.get(app)
        for ((name, id, night) in listOf(Triple("t9-light", "t9_pinyin", false),
            Triple("qwerty-light", "cayboard_pinyin", false), Triple("t9-dark", "t9_pinyin", true),
            Triple("qwerty-dark", "cayboard_pinyin", true), Triple("number-dark", "number", true),
            Triple("symbols-light", "symbols", false), Triple("symbols-dark", "symbols", true),
            Triple("t9-pinyin-dark", "t9_pinyin", true), Triple("t9-dark-return", "t9_pinyin", true),
            Triple("english-dark", "cayboard_english", true), Triple("english-light", "cayboard_english", false),
            Triple("14-dark", "pinyin_14jian", true), Triple("number-light", "number", false),
            Triple("edit-dark", "edit", true), Triple("edit-light", "edit", false))) {
            KeysConfigHelper.setActiveKeyboardSchema(id)
            rule.runOnIdle {
                val typing = name == "t9-pinyin-dark"
                whenever(controller.leftPanelState).thenReturn(if (typing) T9InputController.LeftPanelState.INPUT else T9InputController.LeftPanelState.IDLE)
                whenever(controller.firstOptions).thenReturn(if (typing) listOf("chua", "chu", "ai", "zhuang").map {
                    T9InputController.SyllableOption(it, it.length)
                } else emptyList())
                vm.resetKeyboard(false, id); schema = id; dark = night
            }
            rule.waitForIdle()
            assertSame(sharedAtlas, FixedKeycapAtlas.get(app))
            if (id == "t9_pinyin") {
                rule.onNodeWithText("清空").assertDoesNotExist()
                rule.onNodeWithText("重输").assertDoesNotExist()
            }
            val bounds = rule.onNodeWithTag("keyboard", true).fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.width > 0 && bounds.height > 0)
            val view = rule.activity.window.decorView
            val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            rule.runOnIdle { view.draw(android.graphics.Canvas(full)) }
            val pressedBounds = rule.onNodeWithTag("pressed", true).fetchSemanticsNode().boundsInRoot
            val crop = Bitmap.createBitmap(full, bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), (pressedBounds.top - bounds.top).toInt())
            val sample = Bitmap.createBitmap(full, pressedBounds.left.toInt(), pressedBounds.top.toInt(), pressedBounds.width.toInt(), pressedBounds.height.toInt())
            File(out, "$name-pressed.png").outputStream().use { sample.compress(Bitmap.CompressFormat.PNG, 100, it) }
            File(out, "$name.png").outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
