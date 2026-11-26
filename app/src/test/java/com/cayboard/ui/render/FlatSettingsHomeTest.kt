package com.cayboard.ui.render

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.ui.settings.SettingsMainContent
import com.cayboard.ui.settings.SettingsSearchIndex
import com.cayboard.ui.theme.CayboardTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk=[34],application=Application::class,qualifiers="zh-rCN-w411dp-h900dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FlatSettingsHomeTest {
    @get:Rule val rule=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun homeHasDirectRowsWithoutCategoriesOrDailyCounter() {
        var dark by mutableStateOf(false)
        assertFalse(SettingsSearchIndex.any { it.title=="打字统计" })
        rule.setContent { CayboardTheme(darkTheme=dark) {
            SettingsMainContent(onNavigateToSchema={},onNavigateToLayoutDisplay={},onNavigateToSmartPrediction={},
                onNavigateToSpeechToText={},onNavigateToAbout={})
        } }
        for(mode in listOf(false,true)) {
            rule.runOnIdle {dark=mode}
            listOf("配置选择","输入","智能","系统","打字统计").forEach {rule.onNodeWithText(it).assertDoesNotExist()}
            rule.onNodeWithText("语言与方案").assertExists()
            rule.onNodeWithText("翻译").assertExists()
            rule.onNodeWithText("关于 Cayboard").assertExists()
            System.getenv("CAYBOARD_RENDER_OUT")?.let {dir ->
                val view=rule.activity.window.decorView
                val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
                rule.runOnIdle {view.draw(android.graphics.Canvas(bitmap))}
                File(dir,"flat-settings-home-${if(mode) "dark" else "light"}.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
                }
            }
        }
    }
}
