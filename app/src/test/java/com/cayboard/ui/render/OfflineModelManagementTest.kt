package com.cayboard.ui.render

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.ui.settings.ModelLocalContent
import com.cayboard.ui.theme.CayboardTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "zh-rCN-w360dp-h680dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OfflineModelManagementTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun pageOffersLocalImportWithoutOnlineControls() {
        rule.setContent { CayboardTheme { ModelLocalContent(onBack = {}) } }
        rule.onNodeWithText("模型管理").assertIsDisplayed()
        rule.onNodeWithTag("builtin-t9-reranker").assertIsDisplayed()
        rule.onNodeWithContentDescription("刷新").assertDoesNotExist()
        rule.onNodeWithText("自动更新模型").assertDoesNotExist()
        rule.onNodeWithTag("model-local-list").performScrollToKey("model-import")
        rule.onNodeWithTag("model-import-files").assertIsDisplayed()
        rule.onNodeWithTag("model-import-folder").assertIsDisplayed()
        rule.onNodeWithText("下载线路").assertDoesNotExist()
        System.getenv("CYIME_RENDER_OUT")?.let { directory ->
            rule.runOnIdle {
                val view = rule.activity.window.decorView
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                val file = File(directory, "offline-model-management.png")
                file.parentFile?.mkdirs()
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
    }
}
