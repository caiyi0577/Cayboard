package com.cayboard.ui

import android.app.Application
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.cayboard.settings.KeysConfigHelper
import com.cayboard.settings.SettingsPreferences
import com.cayboard.ui.keyboard.KeyboardCallbacks
import com.cayboard.ui.keyboard.KeyboardView
import com.cayboard.ui.keyboard.SymbolInputMode
import com.cayboard.viewmodel.KeyboardUiState
import com.cayboard.viewmodel.KeyboardViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.tan

/** Exercise the actual layouts AND KeyboardView's competing whole-keyboard cursor gesture. */
class KeyboardSwipeIntegrationTest {
    @get:Rule val rule = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val prefs = SettingsPreferences.getPrefsPublic(app)
    private val preferenceKeys = listOf("cursor_gesture_mode", "symbol_input_mode", "reverse_symbol_swipe",
        "neighbor_correction", "fourteen_letter_swipe", SettingsPreferences.KEY_SPLIT_KEYBOARD)
    private var originalPreferences: Map<String, Any?> = emptyMap()
    private val schema = mutableStateOf("cayboard_pinyin")
    private val commits = mutableListOf<String>()
    private val cursorMoves = mutableListOf<Int>()
    private data class Layout(val schema: String, val label: String, val symbol: String)
    private val layouts = listOf(Layout("cayboard_pinyin", "q", "1"), Layout("pinyin_14jian", "qw", "1"),
        Layout("t9_pinyin", "ABC", "2"))

    @Before fun before() {
        originalPreferences = preferenceKeys.associateWith { prefs.all[it] }
        prefs.edit().putString("cursor_gesture_mode", "KEYBOARD")
            .putString("symbol_input_mode", SymbolInputMode.SWIPE_UP.name)
            .putBoolean("reverse_symbol_swipe", false).putBoolean("neighbor_correction", false)
            .putBoolean("fourteen_letter_swipe", true)
            .putBoolean(SettingsPreferences.KEY_SPLIT_KEYBOARD, false)
            .commit()
        KeysConfigHelper.loadConfig(app)
        KeysConfigHelper.setActiveKeyboardSchema("cayboard_pinyin")
    }

    @After fun after() {
        rule.mainClock.autoAdvance = true
        prefs.edit().also { edit -> originalPreferences.forEach { (name, value) ->
            when (value) {
                is String -> edit.putString(name, value)
                is Boolean -> edit.putBoolean(name, value)
                else -> edit.remove(name)
            }
        } }.commit()
        KeysConfigHelper.setActiveKeyboardSchema("cayboard_pinyin")
    }

    private fun show() {
        rule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                screenWidthDp = 600; screenHeightDp = 800
                orientation = Configuration.ORIENTATION_PORTRAIT
            }
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalConfiguration provides configuration) {
                MaterialTheme {
                    Box(Modifier.requiredSize(600.dp, 700.dp).testTag("swipe-host"), contentAlignment = Alignment.Center) {
                        key(schema.value) {
                            val vm = remember { KeyboardViewModel(app) }
                            KeyboardView(vm,
                                KeyboardUiState(currentSchemaId = schema.value, keyboardHeightDp = 320,
                                    keyboardWidthDp = 360, fixedWidthDp = 360, isDarkTheme = true),
                                KeyboardCallbacks(onKeyPress = { text, _ -> commits += "key:$text" },
                                    onCandidateSelect = {}, onCommitText = { commits += "literal:$it" },
                                    // Do not invoke the input engine if a failed T9 gesture becomes an ordinary digit tap.
                                    onPreeditKeyInput = { commits += "preedit:$it"; true },
                                    onCursorMove = { cursorMoves += it }),
                                modifier = Modifier.size(360.dp, 320.dp).testTag("swipe-keyboard"))
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun activate(layout: Layout, reverse: Boolean = false, mode: SymbolInputMode = SymbolInputMode.SWIPE_UP) {
        rule.runOnIdle {
            prefs.edit().putBoolean("reverse_symbol_swipe", reverse).putString("symbol_input_mode", mode.name).commit()
            KeysConfigHelper.setActiveKeyboardSchema(layout.schema)
            schema.value = layout.schema
        }
        rule.waitForIdle()
        rule.onNodeWithText(layout.label, ignoreCase = true, useUnmergedTree = true).assertIsDisplayed()
        rule.runOnIdle { commits.clear(); cursorMoves.clear() }
    }

    private fun startOf(layout: Layout): Offset {
        val host = rule.onNodeWithTag("swipe-host").fetchSemanticsNode().boundsInRoot
        return rule.onNodeWithText(layout.label, ignoreCase = true, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.center - host.topLeft
    }

    @Test fun diagonalVerticalSelectionSurvivesOtherKeysAndLeavingTheKeyboardForEveryLayout() {
        show()
        for (layout in layouts) for (reverse in listOf(false, true)) for (angle in listOf(0.0, 20.0, 40.0)) {
            activate(layout, reverse)
            val start = startOf(layout)
            val direction = if (reverse) -1f else 1f
            // The first move has a definite vertical intent; the second crosses the entire keyboard.
            val initial = Offset((tan(Math.toRadians(angle)) * 75f).toFloat(), direction * 75f)
            for (outsideX in listOf(580f, 20f)) {
                rule.runOnIdle { commits.clear(); cursorMoves.clear() }
                rule.onNodeWithTag("swipe-host").performTouchInput {
                    down(start); moveTo(start + initial)
                    moveTo(Offset(outsideX, start.y + direction * 80f)); up()
                }
                rule.runOnIdle {
                    val case = "${layout.schema} reverse=$reverse angle=$angle outside=$outsideX"
                    assertEquals(case, listOf("literal:${layout.symbol}"), commits)
                    assertTrue("$case must not move the application's cursor", cursorMoves.isEmpty())
                }
            }
        }
    }

    @Test fun verticalBacktrackingCancelsWithoutTypingOrMovingTheApplicationCursor() {
        show()
        for (layout in layouts) for (reverse in listOf(false, true)) {
            activate(layout, reverse)
            val start = startOf(layout)
            val dy = if (reverse) -80f else 80f
            rule.onNodeWithTag("swipe-host").performTouchInput {
                down(start); moveTo(start + Offset(20f, dy))
                moveTo(Offset(580f, start.y + dy)); moveTo(start); up()
            }
            rule.runOnIdle {
                assertTrue("${layout.schema} reversal must cancel, not type the centre key", commits.isEmpty())
                assertTrue(cursorMoves.isEmpty())
            }
        }
    }

    @Test fun deliberateHorizontalQwertyGestureStillMovesTheCursor() {
        show(); activate(layouts.first())
        val start = startOf(layouts.first())
        rule.onNodeWithTag("swipe-host").performTouchInput {
            down(start); moveTo(start + Offset(90f, 0f)); moveTo(start + Offset(140f, 0f)); up()
        }
        rule.runOnIdle { assertTrue(commits.isEmpty()); assertTrue(cursorMoves.sum() > 0) }
    }

    @Test fun longPressSymbolModeCommitsOnceWithoutTheOldChoicePopup() {
        show()
        for (layout in layouts) {
            activate(layout, mode = SymbolInputMode.LONG_PRESS)
            val start = startOf(layout)
            rule.mainClock.autoAdvance = false
            rule.onNodeWithTag("swipe-host").performTouchInput { down(start) }
            rule.mainClock.advanceTimeBy(320L)
            rule.runOnIdle { assertEquals(listOf("literal:${layout.symbol}"), commits) }
            // 用户 11-08：到点已经输入，气泡显示输入的角标（不是松手才选的菜单），松手消失。
            rule.onNodeWithTag("key-choice-popup").assertExists()
            rule.onNodeWithTag("swipe-host").performTouchInput { moveTo(Offset(580f, start.y + 80f)); up() }
            rule.runOnIdle { assertEquals(listOf("literal:${layout.symbol}"), commits); assertTrue(cursorMoves.isEmpty()) }
            rule.mainClock.advanceTimeByFrame()
            rule.onNodeWithTag("key-choice-popup").assertDoesNotExist()
            rule.mainClock.autoAdvance = true
        }
    }

    @Test fun actualLongPressPopupKeepsTheOriginalTouchStreamAndCommitsUppercaseExactlyOnce() {
        show(); activate(layouts.first())
        val start = startOf(layouts.first())
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("swipe-host").performTouchInput { down(start) }
        rule.mainClock.advanceTimeBy(450L)
        rule.onNodeWithTag("key-choice-popup").assertIsDisplayed()
        val popup = rule.onNodeWithTag("key-choice-popup").fetchSemanticsNode()
        val host = rule.onNodeWithTag("swipe-host").fetchSemanticsNode()
        val upperChoice = popup.positionOnScreen + Offset(popup.size.width * .8f, popup.size.height * .3f) - host.positionOnScreen
        rule.runOnIdle { assertTrue(commits.isEmpty()) }
        // Continue the original down on the keyboard's owner even though the popup is a new window.
        rule.onNodeWithTag("swipe-host").performTouchInput { moveTo(upperChoice); up() }
        rule.runOnIdle { assertEquals(listOf("literal:Q"), commits); assertTrue(cursorMoves.isEmpty()) }
        // Release already cleared the bubble state; with the clock paused, removing the popup needs one frame.
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("key-choice-popup").assertDoesNotExist()
        rule.mainClock.autoAdvance = true
        rule.onNodeWithTag("swipe-host").performTouchInput { down(start); up() }
        rule.runOnIdle { assertEquals(listOf("literal:Q", "key:q"), commits) }
    }
}
