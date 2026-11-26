package com.cayboard.ui.render

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.clipboard.ClipboardItem
import com.cayboard.settings.ClipboardWordSegmenter
import com.cayboard.settings.KeysConfigHelper
import com.cayboard.ui.keyboard.*
import com.cayboard.ui.menubar.ClipboardBoardView
import com.cayboard.ui.theme.*
import com.cayboard.viewmodel.KeyboardViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "zh-rCN-w420dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ClipboardRefreshRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun actualClipboardAndSplitViewsPreserveOriginalAndSelection() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        KeysConfigHelper.loadConfig(app); AppFonts.initialize(app); KeyboardThemes.reload(app)
        runBlocking { ClipboardWordSegmenter.prewarm(app) }
        val vm = mock<KeyboardViewModel>()
        val original = "\n\n会员健身服务协议\n\n\n请妥善保管本协议。\n"
        var input by mutableStateOf("今天天气不错\nHello 👨‍👩‍👧‍👦！")
        var page by mutableStateOf(0); var dark by mutableStateOf(true)
        var pasted = ""; var confirmed = ""; var back = 0
        val history = listOf(ClipboardItem(1,original,timestamp=100), ClipboardItem(2,"1234567890123",timestamp=90),
            ClipboardItem(3,"https://example.org/",timestamp=80),ClipboardItem(4,"工作安排：下午三点开会",timestamp=70))
        rule.setContent {
            val config = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if(dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(LocalConfiguration provides config, LocalKeyboardInputPreferences provides KeyboardInputPreferences()) {
                CayboardTheme(darkTheme=dark) {
                    val palette = resolveKeyboardPalette(FixedKeyboardStyle.ID,dark)
                    Box(Modifier.requiredSize(400.dp,330.dp).background(palette.background).testTag("clipboard-review")) {
                        if(page==0) ClipboardBoardView(history,onSelectText={pasted=it},onSplit={_,_->},onRemove={},
                            modifier=Modifier.fillMaxSize(),listLayout=true,onOpenSearch={})
                        else SplitWordsView(input,palette.background,vm,onBack={back++},onConfirmText={confirmed=it},modifier=Modifier.fillMaxSize())
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("clipboard-card:text:1").performClick()
        rule.runOnIdle { assertEquals(original,pasted) }
        save("clipboard-refreshed-dark.png")
        rule.onNodeWithTag("clipboard-more").performClick()
        rule.onNodeWithTag("clipboard-select").performClick()
        rule.onNodeWithTag("clipboard-card:text:1").performClick()
        rule.onNodeWithTag("clipboard-card:text:1").assertIsSelected()
        save("clipboard-selected-dark.png")
        rule.onNodeWithTag("clipboard-exit-selection").performClick()
        rule.runOnIdle { dark=false };rule.waitForIdle();save("clipboard-refreshed-light.png")
        rule.runOnIdle { dark=true;page=1 }
        rule.waitUntil(8000) { rule.onAllNodesWithText("今天天气").fetchSemanticsNodes().isNotEmpty() }
        val originalBounds=rule.onNodeWithTag("split-part:0").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("split-part:0").performTouchInput { click() }
        val selectedBounds=rule.onNodeWithTag("split-part:0").fetchSemanticsNode().boundsInRoot
        assertEquals(originalBounds.width,selectedBounds.width,.01f)
        assertEquals(originalBounds.height,selectedBounds.height,.01f)
        rule.onNodeWithTag("split-copy").performClick()
        rule.runOnIdle {
            val clipboard=app.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            assertEquals("今天天气",clipboard.primaryClip!!.getItemAt(0).text.toString())
        }
        rule.onNodeWithTag("split-pin").assertDoesNotExist()
        save("split-refreshed-dark-selected.png")
        rule.onNodeWithTag("split-select-all").performClick()
        rule.onNodeWithTag("split-invert").performClick()
        rule.onNodeWithTag("split-confirm").assertIsNotEnabled()
        val first=rule.onNodeWithTag("split-part:0").fetchSemanticsNode().boundsInRoot.center
        val third=rule.onNodeWithTag("split-part:2").fetchSemanticsNode().boundsInRoot.center
        rule.onRoot().performTouchInput { down(first);moveTo(third,delayMillis=200);up() }
        rule.onNodeWithTag("split-part:0").assertIsSelected()
        rule.onNodeWithTag("split-part:1").assertIsSelected()
        rule.onNodeWithTag("split-part:2").assertIsSelected()
        rule.onNodeWithTag("split-mode:true").performClick()
        rule.waitUntil(8000) { rule.onAllNodesWithText("今").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("split-part:0").performClick()
        save("split-refreshed-characters.png")
        rule.onNodeWithTag("split-select-all").performClick()
        rule.onNodeWithTag("split-confirm").performClick()
        rule.runOnIdle { assertEquals(input,confirmed);assertEquals(1,back);dark=false }
        rule.waitForIdle();save("split-refreshed-light.png")
        rule.runOnIdle { input = "研究生命起源。".repeat(500); dark=true }
        rule.waitUntil(8000) { rule.onAllNodesWithText("研究").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("split-part:500").assertDoesNotExist()
        rule.onNodeWithTag("split-words-region").performScrollToIndex(30)
        rule.onNodeWithTag("split-part:720").assertExists()
        rule.onNodeWithTag("split-words-region").performTouchInput { swipeUp(durationMillis=300) }
        rule.onNodeWithTag("split-confirm").assertIsNotEnabled()
        rule.onNodeWithTag("split-select-all").performClick()
        rule.onNodeWithTag("split-confirm").performClick()
        rule.runOnIdle { assertEquals(input,confirmed); assertEquals(2,back) }
    }
    private fun save(name:String) {
        val out=System.getenv("CAYBOARD_RENDER_OUT")?:return
        File(out).mkdirs();rule.waitForIdle()
        val bounds=rule.onNodeWithTag("clipboard-review",true).fetchSemanticsNode().boundsInRoot
        val root=rule.activity.window.decorView
        val full=Bitmap.createBitmap(root.width,root.height,Bitmap.Config.ARGB_8888)
        rule.runOnIdle {root.draw(android.graphics.Canvas(full))}
        val crop=Bitmap.createBitmap(full,bounds.left.toInt(),bounds.top.toInt(),bounds.width.toInt(),bounds.height.toInt())
        File(out,name).outputStream().use {crop.compress(Bitmap.CompressFormat.PNG,100,it)}
        crop.recycle();full.recycle()
    }
}
