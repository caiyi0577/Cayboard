package com.cayboard.ui.render

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.keyboard.ToolbarAction
import com.cayboard.keyboard.ToolbarButton
import com.cayboard.keyboard.ToolbarButtonItem
import com.cayboard.ui.keyboard.ToolbarButtonIcon
import com.cayboard.ui.keyboard.CandidateBar
import com.cayboard.ui.keyboard.CandidateBarCallbacks
import com.cayboard.ui.keyboard.CandidateBarState
import com.cayboard.ui.keyboard.CandidateBarVisuals
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ToolbarIconRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun translateUsesAnIconAndTraditionalSwitchUsesItsGlyph() {
        rule.setContent { Row {
            ToolbarButtonIcon(ToolbarButtonItem.Builtin(ToolbarButton.TRANSLATE), Color.Black, Modifier.size(24.dp))
            ToolbarButtonIcon(ToolbarButtonItem.Builtin(ToolbarButton.TRADITIONAL), Color.Black, Modifier.size(24.dp), active = true)
        } }
        rule.onNodeWithContentDescription("翻译").assertExists()
        rule.onNodeWithText("译", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("繁体").assertExists()
    }

    @Test fun leadingButtonUsesTheRoundedAppIconAndOpensTheMenu() {
        val dark = Color(0xFF1E1E1E)
        val light = Color(0xFFE4E7EC)
        var opens = 0
        val tools = ToolbarButton.DEFAULT_VISIBLE.map { button ->
            ToolbarAction(ToolbarButtonItem.Builtin(button)) {}
        }
        rule.setContent {
            Column(Modifier.width(360.dp)) {
                for (background in listOf(dark, light)) {
                    CandidateBar(
                        state = CandidateBarState.Idle, toolbarActions = tools,
                        visuals = CandidateBarVisuals(background, if (background == dark) Color.White else Color.Black,
                            Color.Gray, isDarkTheme = background == dark),
                        callbacks = CandidateBarCallbacks(onCandidateSelect = {}, onLogoClick = { opens++ }, onHideKeyboard = {}),
                        showPreeditPreview = false,
                    )
                }
            }
        }
        val logos = rule.onAllNodesWithContentDescription("展开菜单", useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(2, logos.size)
        val view = rule.activity.window.decorView
        val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnIdle { view.draw(android.graphics.Canvas(image)) }
        for ((index, logo) in logos.withIndex()) {
            val bounds = logo.boundsInRoot
            fun pixel(x: Float, y: Float) = image.getPixel(
                (bounds.left + x * bounds.width).toInt(), (bounds.top + y * bounds.height).toInt())
            assertEquals("The C uses the app icon's first keycap color", 0xFF353E42.toInt(), pixel(.28f, .28f))
            assertEquals("The opening in the C shows the icon's tile", dark.toArgb(), pixel(.5f, .5f))
            assertEquals("The corners of the tile are clipped", (if (index == 0) dark else light).toArgb(), pixel(0f, 0f))
            assertNotEquals("The tile edge remains visible", (if (index == 0) dark else light).toArgb(), pixel(.5f, 0f))
        }
        rule.onAllNodesWithTag("toolbar-leading")[0].performClick()
        rule.runOnIdle { assertEquals(1, opens) }
        System.getenv("CYIME_RENDER_OUT")?.let { dir ->
            val out = File(dir, "toolbar-app-icon.png")
            out.parentFile?.mkdirs()
            val height = (logos.last().boundsInRoot.bottom + logos.first().boundsInRoot.top).toInt()
            Bitmap.createBitmap(image, 0, 0, with(rule.density) { 360.dp.roundToPx() }.coerceAtMost(image.width), height)
                .let { crop -> out.outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
    }
}
