package com.cayboard.ui.render

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.ui.common.UiNode
import com.cayboard.ui.common.UiNodeType
import com.cayboard.translate.TranslationLanguages
import com.cayboard.ui.keyboard.OptionMenuCard
import com.cayboard.ui.keyboard.ToolPanelBar
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
class TranslationLanguageMenuTest {
    @get:Rule val rule=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val languages=TranslationLanguages.all.map { it.name }
    @Test fun sourceAndTargetMenusScrollBothWaysWithoutSelectingDuringSwipe() = exerciseLiveBar(false)
    @Test @Config(qualifiers="zh-rCN-w800dp-h360dp-mdpi")
    fun landscapeDarkMenusScrollAndReopenAtCurrentLanguage() = exerciseLiveBar(true)
    private fun exerciseLiveBar(dark: Boolean) {
        var source by mutableStateOf("自动检测")
        var target by mutableStateOf("英文")
        val choices=mutableListOf<Pair<String,String>>()
        rule.setContent { CayboardTheme(darkTheme=dark) {
            Box(Modifier.fillMaxSize(), contentAlignment=Alignment.BottomCenter) {
                ToolPanelBar(null,"翻译",false,false,Color.White,Color.Cyan,Color.DarkGray,
                    {},{},{},{}, selects=listOf(
                        UiNode(UiNodeType.SELECT,"source","原文语言",source,options=listOf("自动检测")+languages),
                        UiNode(UiNodeType.SELECT,"target","翻译成",target,options=languages)),
                    onSelect={key,value -> choices.add(key to value); if(key=="source") source=value else target=value})
            }
        } }
        for(key in listOf("source","target")) {
            val before=choices.size
            rule.onNodeWithTag("live-bar-lang:$key").performClick()
            repeat(12) { rule.onNodeWithTag("option-menu-options").performTouchInput { swipeUp() } }
            assertEquals(before,choices.size)
            rule.onNodeWithTag("option-menu-row:粤语").assertIsDisplayed().performClick()
            assertEquals(key to "粤语",choices.last())
            rule.onNodeWithTag("option-menu").assertDoesNotExist()
            rule.onNodeWithTag("live-bar-lang:$key").performClick()
            rule.onNodeWithTag("option-menu-row:粤语").assertIsDisplayed().assertIsSelected()
            repeat(12) { rule.onNodeWithTag("option-menu-options").performTouchInput { swipeDown() } }
            rule.onNodeWithTag("option-menu-row:简体中文").assertIsDisplayed().performClick()
            assertEquals(key to "简体中文",choices.last())
            assertEquals(before+2,choices.size)
        }
    }
    @Test fun boundedMenuRendersInBothAppearances() {
        var dark by mutableStateOf(false)
        rule.setContent { CayboardTheme(darkTheme=dark) {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                OptionMenuCard("翻译成",languages,"英文",onChoose={},maxHeight=240.dp)
            }
        } }
        for(mode in listOf(false,true)) {
            rule.runOnIdle { dark=mode }
            rule.onNodeWithTag("option-menu").assertHeightIsEqualTo(240.dp)
            rule.onNodeWithTag("option-menu-options").assert(hasScrollAction())
            System.getenv("CAYBOARD_RENDER_OUT")?.let {dir ->
                val view=rule.activity.window.decorView
                val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
                rule.runOnIdle {view.draw(android.graphics.Canvas(bitmap))}
                File(dir,"translation-language-menu-${if(mode) "dark" else "light"}.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
                }
            }
        }
    }
}
