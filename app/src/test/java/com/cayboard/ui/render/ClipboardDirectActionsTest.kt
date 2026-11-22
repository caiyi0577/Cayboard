package com.cayboard.ui.render

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.clipboard.ClipboardItem
import com.cayboard.settings.KeysConfigHelper
import com.cayboard.ui.menubar.ClipboardBoardView
import com.cayboard.ui.theme.CayboardTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk=[34],application=Application::class,qualifiers="zh-rCN-w411dp-h650dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ClipboardDirectActionsTest {
    @get:Rule val rule=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val long="第一行文字\n第二行文字\n第三行必须截断"
    private var rows by mutableStateOf(listOf(ClipboardItem(1,long,timestamp=3),ClipboardItem(2,"第二条记录",timestamp=2),ClipboardItem(3,"https://example.com",timestamp=1)))
    private val removed=mutableListOf<Long>()
    private val split=mutableListOf<String>()
    private val pasted=mutableListOf<String>()
    private var dark by mutableStateOf(false)
    private var list by mutableStateOf(true)
    private var settings=0
    private fun content() {
        KeysConfigHelper.loadConfig(ApplicationProvider.getApplicationContext<Application>())
        rule.setContent { CayboardTheme(darkTheme=dark) { Surface {
            Box(Modifier.fillMaxWidth().height(365.dp).background(Color.Magenta)) {
                ClipboardBoardView(rows,onSelectText={pasted+=it},onSplit={text,_->split+=text},
                    onRemove={gone->removed+=gone.map {it.id};rows=rows.filterNot {r->r.id in gone.map {it.id}}},
                    listLayout=list,onListLayoutChange={list=it},
                    onOpenSearch={},onOpenSettings={settings++})
            }
        } } }
    }
    @Test fun panelBackgroundHidesUnderlyingKeyboardInBothAppearances() {
        content()
        listOf(false,true).forEach { mode ->
            rule.runOnIdle {dark=mode}
            val bounds=rule.onNodeWithTag("clipboard-board").fetchSemanticsNode().boundsInRoot
            val view=rule.activity.window.decorView
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            rule.runOnIdle {view.draw(android.graphics.Canvas(bitmap))}
            val pixel=bitmap.getPixel(bounds.center.x.toInt(),bounds.bottom.toInt()-2)
            assertNotEquals("Panel bottom must cover underlying keys",Color.Magenta.toArgb(),pixel)
            assertEquals(255,android.graphics.Color.alpha(pixel))
            capture(if(mode) "clipboard-background-dark" else "clipboard-background-light")
        }
    }
    @Test fun longPressSplitsOriginalTextImmediatelyWithoutMenuOrPaste() {
        content()
        rule.onNodeWithTag("clipboard-card:text:1").performTouchInput { longClick() }
        rule.runOnIdle { assertEquals(listOf(long),split);assertTrue(pasted.isEmpty());assertTrue(removed.isEmpty()) }
        listOf("固定","取消固定","编辑","更多操作","分词").forEach { rule.onNodeWithText(it).assertDoesNotExist() }
    }
    @Test fun leftSwipeOnlyRevealsAndTwoButtonClicksDelete()=swipeAndConfirm(false)
    @Test fun rightSwipeOnlyRevealsAndTwoButtonClicksDelete()=swipeAndConfirm(true)
    private fun swipeAndConfirm(right:Boolean) {
        content()
        rule.onNodeWithTag("clipboard-card:text:1").performTouchInput { if(right) swipeRight() else swipeLeft() }
        rule.onNodeWithTag("clipboard-delete:1").assertIsDisplayed().performClick()
        rule.runOnIdle {assertTrue(removed.isEmpty());assertEquals(3,rows.size);assertTrue(pasted.isEmpty())}
        rule.onNodeWithText("确认删除").assertExists()
        val card=rule.onNodeWithTag("clipboard-card:text:1").getUnclippedBoundsInRoot()
        val preview=rule.onNodeWithText(long,useUnmergedTree=true).getUnclippedBoundsInRoot()
        assertTrue("Record text must remain inside its card",preview.left>=card.left && preview.right<=card.right)

        capture(if(right) "clipboard-delete-right" else "clipboard-delete-left")
        rule.onNodeWithTag("clipboard-delete:1").performClick()
        rule.runOnIdle {assertEquals(listOf(1L),removed);assertEquals(2,rows.size)}
        rule.onNodeWithTag("clipboard-card:text:1").assertDoesNotExist()
    }
    @Test fun anotherRecordResetsConfirmationAndSlidingNeverDeletes() {
        content()
        rule.onNodeWithTag("clipboard-card:text:1").performTouchInput {swipeLeft()}
        rule.onNodeWithTag("clipboard-delete:1").performClick()
        rule.onNodeWithTag("clipboard-card:text:2").performTouchInput {swipeRight()}
        rule.onNodeWithTag("clipboard-card:text:1").performTouchInput {swipeLeft()}
        rule.onNodeWithTag("clipboard-delete:1").performClick()
        rule.runOnIdle {assertTrue(removed.isEmpty());assertTrue(pasted.isEmpty())}
        rule.onNodeWithTag("clipboard-delete:1").performClick()
        rule.runOnIdle {assertEquals(listOf(1L),removed)}
    }
    @Test fun aClipboardRefreshCancelsTheOldDeleteConfirmation() {
        content()
        rule.onNodeWithTag("clipboard-card:text:1").performTouchInput {swipeLeft()}
        rule.onNodeWithTag("clipboard-delete:1").performClick()
        rule.runOnIdle {rows=rows+ClipboardItem(4,"新复制的内容",timestamp=4)}
        rule.onNodeWithTag("clipboard-delete:1").assertDoesNotExist()
        rule.onNodeWithTag("clipboard-card:text:1").performTouchInput {swipeLeft()}
        rule.onNodeWithTag("clipboard-delete:1").performClick()
        rule.runOnIdle {assertTrue(removed.isEmpty())}
    }

    @Test fun actionsAreInlineAndRecordsHaveTwoLinesInBothAppearances() {
        content()
        listOf("clipboard-layout","clipboard-search","clipboard-select","clipboard-settings").forEach { rule.onNodeWithTag(it).assertExists() }
        listOf("更多操作","自动关闭","清空").forEach { rule.onNodeWithText(it).assertDoesNotExist() }
        listOf("clipboard-clear","clipboard-close-after-paste").forEach {rule.onNodeWithTag(it).assertDoesNotExist()}
        listOf(false,true).forEach { mode ->
            rule.runOnIdle {dark=mode}
            val layouts=mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(long,useUnmergedTree=true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {it(layouts)}
            assertEquals(2,layouts.single().lineCount)
            assertTrue(layouts.single().isLineEllipsized(1))
            capture(if(mode) "clipboard-direct-dark" else "clipboard-direct-light")
        }
    }
    @Test fun multiSelectDeletionStaysConfirmedAndSettingsRemainsAccessible() {
        content()
        rule.onNodeWithTag("clipboard-select").performClick()
        rule.onNodeWithTag("clipboard-card:text:2").performClick()
        rule.onNodeWithTag("clipboard-delete-selected").performClick()
        rule.runOnIdle {assertTrue(removed.isEmpty())}
        rule.onNodeWithTag("clipboard-delete-selected").performClick()
        rule.runOnIdle {assertEquals(listOf(2L),removed)}
        rule.onNodeWithTag("clipboard-clear").assertDoesNotExist()
        rule.onNodeWithTag("clipboard-settings").performClick()
        rule.runOnIdle {assertEquals(1,settings);assertEquals(2,rows.size)}
    }
    @Test fun verticalGestureScrollsWithoutRevealingOrDeleting() {
        rows=(1L..40L).map {ClipboardItem(it,"条目$it",timestamp=100-it)}
        content()
        rule.onNodeWithTag("clipboard-records").performTouchInput {swipeUp()}
        rule.runOnIdle {assertTrue(removed.isEmpty());assertTrue(pasted.isEmpty())}
        rule.onAllNodesWithText("条目1").assertCountEquals(0)
        rule.onAllNodesWithText("删除").assertCountEquals(0)
    }
    private fun capture(name:String) {
        System.getenv("CYIME_RENDER_OUT")?.let {dir->
            val view=rule.activity.window.decorView
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            rule.runOnIdle {view.draw(android.graphics.Canvas(bitmap))}
            File(dir).mkdirs();File(dir,"$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        }
    }
}
