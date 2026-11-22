package com.cayboard.ui.render

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.ui.keyboard.*
import com.cayboard.ui.settings.HardwareKeyboardSettingsContent
import com.cayboard.ui.settings.SettingsSearchIndex
import com.cayboard.ui.theme.CayboardTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk=[34], application=Application::class, qualifiers="zh-rCN-w411dp-h900dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HardwareSideToolbarTest {
    @get:Rule val rule=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun startsVerticalAndDragsBetweenSidesWithoutChangingShapeOrClickingActions() = exercise(false)
    @Test @Config(qualifiers="zh-rCN-w800dp-h300dp-mdpi")
    fun shortLandscapeKeepsEveryActionAndRemainsVertical() = exercise(true)
    private fun exercise(dark: Boolean) {
        AppFonts.initialize(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        // An old bottom-centred position must not restore the old bottom bar.
        var position by mutableStateOf(HardwareToolbarPosition())
        var clicks=0
        rule.setContent { CayboardTheme(darkTheme=dark) {
            Box(Modifier.fillMaxSize().background(if(dark) Color(0xFF202020) else Color(0xFFF5F5F5))) {
                HardwareKeyboardToolbar({clicks++},{clicks++},{clicks++},{clicks++},
                    if(dark) Color(0xFF303030) else Color.White, if(dark) Color.White else Color.Black,
                    {},position,{position=it},onVoice={clicks++},avoidEditor=false)
            }
        } }
        val bar=rule.onNodeWithTag("hardware-keyboard-toolbar")
        bar.assertWidthIsEqualTo(56.dp)
        for(tag in listOf("keyboard","emoji","clipboard","voice","language")) {
            rule.onNodeWithTag("hardware-toolbar-$tag").assertIsDisplayed()
        }
        fun bounds()=bar.fetchSemanticsNode().boundsInRoot
        val original=bounds()
        assertTrue(original.height>original.width*3)
        val density=rule.density.density
        val hostWidth=rule.activity.resources.configuration.screenWidthDp*density
        assertEquals(hostWidth-8*density,original.right,2f)
        rule.onNodeWithTag("hardware-toolbar-drag").performTouchInput {
            down(center); moveBy(Offset(-hostWidth*.7f,-30*density))
        }
        rule.waitForIdle()
        bar.assertWidthIsEqualTo(56.dp)
        assertTrue(bounds().height>bounds().width*3)
        shot("hardware-side-${if(dark) "landscape-dark" else "portrait-light"}-drag")
        rule.onNodeWithTag("hardware-toolbar-drag").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(0f,position.xFraction,0f)
        assertEquals(8*density,bounds().left,2f)
        assertEquals(0,clicks)
        shot("hardware-side-${if(dark) "landscape-dark" else "portrait-light"}-left")
        rule.onNodeWithTag("hardware-toolbar-drag").performTouchInput {
            down(center); moveBy(Offset(hostWidth*.7f,0f)); up()
        }
        rule.waitForIdle()
        assertEquals(1f,position.xFraction,0f)
        bar.assertWidthIsEqualTo(56.dp)
        for(tag in listOf("keyboard","emoji","clipboard","voice")) rule.onNodeWithTag("hardware-toolbar-$tag").performClick()
        assertEquals(4,clicks)
        shot("hardware-side-${if(dark) "landscape-dark" else "portrait-light"}-right")
    }
    @Test fun retiredDockingSettingsHaveNoVisibleOrSearchEntry() {
        assertFalse(SettingsSearchIndex.any { it.title in listOf("靠边停靠","底部候选") })
        rule.setContent { CayboardTheme { HardwareKeyboardSettingsContent({}) } }
        rule.onNodeWithText("靠边停靠").assertDoesNotExist()
        rule.onNodeWithText("底部候选").assertDoesNotExist()
    }
    private fun shot(name: String) {
        System.getenv("CYIME_RENDER_OUT")?.let {dir ->
            val view=rule.activity.window.decorView
            val image=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            rule.runOnIdle {view.draw(android.graphics.Canvas(image))}
            File(dir,"$name.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}
        }
    }
}
