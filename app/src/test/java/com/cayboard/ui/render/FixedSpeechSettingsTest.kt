package com.cayboard.ui.render

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.ui.settings.SpeechToTextSettingsContent
import com.cayboard.ui.theme.CayboardTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk=[34],application=Application::class,qualifiers="zh-rCN-w411dp-h900dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FixedSpeechSettingsTest {
    @get:Rule val rule=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun fixedModelOnlyHasItsNameAndVersion() {
        var dark by mutableStateOf(false)
        rule.setContent { CayboardTheme(darkTheme=dark) {SpeechToTextSettingsContent({})} }
        for(mode in listOf(false,true)) {
            rule.runOnIdle {dark=mode}
            listOf("纠错","本地识别","保存语音调优录音","在线","SenseVoice","Paraformer","下载").forEach {
                rule.onAllNodes(hasText(it,substring=true)).assertCountEquals(0)
            }
            rule.onNodeWithText("X-ASR INT8").performScrollTo().assertIsDisplayed()
            rule.onNodeWithText("版本：2026-06-03").assertIsDisplayed()
            System.getenv("CYIME_RENDER_OUT")?.let {dir ->
                val view=rule.activity.window.decorView
                val image=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
                rule.runOnIdle {view.draw(android.graphics.Canvas(image))}
                File(dir,"fixed-speech-settings-${if(mode) "dark" else "light"}.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}
            }
        }
    }
}
