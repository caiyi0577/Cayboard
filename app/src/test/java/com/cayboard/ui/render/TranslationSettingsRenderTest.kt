package com.cayboard.ui.render

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.ui.settings.TranslationSettingsContent
import com.cayboard.ui.theme.CayboardTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk=[34],application=Application::class,qualifiers="zh-rCN-w411dp-h900dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TranslationSettingsRenderTest {
 @get:Rule val rule=createAndroidComposeRule<androidx.activity.ComponentActivity>()
 @Test fun translationIsUnderSmartAndOnlyOfflineLanguagesAreConfigurable() {
  val entry=com.cayboard.ui.settings.SettingsSearchIndex.single {it.title=="翻译"}
  assertTrue(entry.path.startsWith("智能"))
  var dark by mutableStateOf(false)
  rule.setContent {CayboardTheme(darkTheme=dark) {TranslationSettingsContent(onBack={})}}
  listOf(false,true).forEach {mode ->
   rule.runOnIdle {dark=mode}
   listOf("原文语言","翻译成","Hy-MT2-1.8B","版本：2（1.25bit） · 大小：440.5 MiB").forEach {rule.onNodeWithText(it).assertExists()}
   listOf("API 密钥","接口地址","在线模型","测试接口","已就绪","本地处理","中译英","英译中").forEach {rule.onNodeWithText(it).assertDoesNotExist()}
   System.getenv("CYIME_RENDER_OUT")?.let {dir ->
    val view=rule.activity.window.decorView;val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
    rule.runOnIdle {view.draw(android.graphics.Canvas(bitmap))}
    File(dir,"translation-settings-${if(mode) "dark" else "light"}.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
   }
  }
  rule.onNodeWithText("翻译成").performClick()
  rule.onNodeWithText("日语").assertExists()
  rule.onNodeWithText("日语").performClick()
  rule.onNodeWithText("日语").assertExists()
 }
}
