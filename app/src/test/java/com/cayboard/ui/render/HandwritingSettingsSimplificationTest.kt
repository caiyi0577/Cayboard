package com.cayboard.ui.render

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.ui.settings.HandwritingSettingsContent
import com.cayboard.ui.theme.CayboardTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "zh-rCN-w400dp-h850dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HandwritingSettingsSimplificationTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun modelManagementRemainsWithoutTrainingOrContribution() {
        rule.setContent { CayboardTheme(darkTheme = true) { HandwritingSettingsContent(onBack = {}) } }
        rule.onNodeWithText("已安装的手写模型").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("改进手写").assertDoesNotExist()
        rule.onNodeWithText("手写训练").assertDoesNotExist()
        rule.onNodeWithText("贡献手写样本").assertDoesNotExist()
        System.getenv("CYIME_RENDER_OUT")?.let { dir ->
            rule.runOnIdle {
                val root = rule.activity.window.decorView
                val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                root.draw(Canvas(bitmap))
                File(dir, "handwriting-settings-no-training.png").apply { parentFile?.mkdirs() }
                    .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
    }
}
