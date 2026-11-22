package com.cayboard.ui.keyboard

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CharacterPickerTouchTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val typed = mutableListOf<String>()
    private val previews = mutableListOf<SwipeState>()
    private fun show(text: String = "q", badge: String = "1", lower: String = "!") {
        AppFonts.initialize(rule.activity)
        rule.setContent {
            CompositionLocalProvider(LocalKeyboardInputActions provides KeyboardInputActions(onCommitExactText = { typed += it }),
                LocalKeyboardInputPreferences provides KeyboardInputPreferences()) {
                Column(Modifier.fillMaxSize().padding(top = 180.dp)) {
                    SwipeableKeyButton(text = text, badgeText = badge, symbolInputText = badge,
                        symbolInputDownText = lower, backgroundColor = Color.DarkGray, textColor = Color.White,
                        onClick = { typed += text }, modifier = Modifier.size(80.dp, 60.dp).testTag("held-key"),
                        onSwipeStateChange = { state, _ -> previews += state })
                }
            }
        }
    }
    private fun hold() {
        rule.onNodeWithTag("held-key").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(450)
        rule.waitForIdle()
    }
    @Test fun tappingNineKeyNeverPublishesAPreview() {
        show("def", "3")
        val key = rule.onNodeWithTag("held-key")
        key.performTouchInput { down(center); advanceEventTime(80) }
        rule.onNodeWithTag("character-picker").assertDoesNotExist()
        key.performTouchInput { up() }
        rule.runOnIdle {
            assertTrue(previews.isEmpty())
            assertEquals(listOf("def"), typed)
        }
    }
    @Test fun holdShowsAllChoicesAndCommitsOnlyOnRelease() {
        show(); hold()
        assertTrue(typed.isEmpty())
        rule.onNodeWithTag("character-picker").assertExists()
        rule.onNodeWithTag("character-choice-3").assertExists()
        rule.onNodeWithTag("held-key").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(listOf("1"), typed)
    }
    @Test fun slideChoosesPrintedSymbolAndTapStaysNormal() {
        show(); hold()
        val key = rule.onNodeWithTag("held-key").fetchSemanticsNode().boundsInRoot
        val layout = characterPickerLayout(key, rule.activity.window.decorView.width.toFloat(),
            rule.activity.resources.displayMetrics.density, 4)
        val target = Offset(layout.bounds.left + 3.5f * layout.cellWidth, layout.bounds.center.y)
        // Popup coordinates are window coordinates, matching the root's origin in this activity.
        rule.onNodeWithTag("held-key").performTouchInput { moveTo(target - key.topLeft); up() }
        rule.waitForIdle()
        assertEquals(listOf("!"), typed)
        rule.onNodeWithTag("held-key").performTouchInput { click() }
        rule.waitForIdle()
        assertEquals(listOf("!", "q"), typed)
    }
    @Test fun leavingPickerCancelsAndNineKeyIncludesAllLetters() {
        show("pqrs", "7", "+"); hold()
        rule.onNodeWithTag("character-choice-9").assertExists()
        rule.onNodeWithText("S").assertExists()
        rule.onNodeWithTag("held-key").performTouchInput { moveTo(Offset(-200f, -500f)); up() }
        rule.waitForIdle()
        assertTrue(typed.isEmpty())
    }
    @Test fun slideSelectsUppercaseFromSecondRowWithoutChangingCase() {
        show("pqrs", "7", "+"); hold()
        assertTrue(typed.isEmpty())
        val key = rule.onNodeWithTag("held-key").fetchSemanticsNode().boundsInRoot
        val layout = characterPickerLayout(key, rule.activity.window.decorView.width.toFloat(),
            rule.activity.resources.displayMetrics.density, 10)
        val index = 8 // S in [7, p, q, r, s, P, Q, R, S, +].
        val target = Offset(layout.bounds.left + (index % layout.columns + .5f) * layout.cellWidth,
            layout.bounds.top + (index / layout.columns + .5f) * layout.cellHeight)
        rule.onNodeWithTag("held-key").performTouchInput { moveTo(target - key.topLeft); up() }
        rule.waitForIdle()
        assertEquals(listOf("S"), typed)
    }
    @Test fun renderSelectionPanel() {
        rule.setContent {
            Column(Modifier.padding(20.dp).testTag("picker-preview")) {
                val choices = keyCharacterChoices("pqrs", null, "7", "7", null, emptyList())
                val layout = characterPickerLayout(androidx.compose.ui.geometry.Rect(0f, 300f, 80f, 400f), 760f, 2f, choices.size)
                CharacterPickerPanel(choices, 8, layout,
                    Color(0xFF30383D), Color.White, Color(0xFFB8D5DE))
                Spacer(Modifier.height(12.dp))
                CharacterPickerPanel(keyCharacterChoices("q", "q", "1", "1", "!", emptyList()), 2,
                    characterPickerLayout(androidx.compose.ui.geometry.Rect(0f, 300f, 80f, 400f), 760f, 2f, 4),
                    Color(0xFFF1F3F5), Color(0xFF252B30), Color(0xFFB8D5DE))
            }
        }
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnIdle { view.draw(android.graphics.Canvas(bitmap)) }
        val out = File(System.getenv("CYIME_RENDER_OUT") ?: "build/fixed-keyboard-renders").apply { mkdirs() }
        File(out, "character-picker.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
