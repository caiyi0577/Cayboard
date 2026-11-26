package com.cayboard.ui.render

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.ui.settings.AboutContent
import com.cayboard.ui.settings.PrivacyPolicyContent
import com.cayboard.ui.theme.KeyboardThemes
import com.cayboard.ui.theme.CayboardTheme
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 「关于」和隐私政策在浅色和深色下的样子，用真实设置页代码在电脑上渲染（每种一张图：左隐私政策、右关于）。
 * 只在设置了环境变量 CAYBOARD_RENDER_OUT=<目录> 时出图（平时跳过，不进普通单测）。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "zh-rCN-w880dp-h2600dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsHomeRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun renderLightAboutAndPrivacy() = render(dark = false)

    @Test fun renderDarkAboutAndPrivacy() = render(dark = true)

    private fun render(dark: Boolean) {
        val out = System.getenv("CAYBOARD_RENDER_OUT")
        assumeTrue(!out.isNullOrEmpty())
        val search = com.cayboard.ui.settings.SettingsSearchIndex
        assertTrue(search.none { it.title in setOf("更新内容", "鸣谢") })
        assertTrue(search.any { it.title == "隐私政策" })
        val context = ApplicationProvider.getApplicationContext<Context>()
        KeyboardThemes.initFromConfig(context)
        rule.setContent {
            CayboardTheme(darkTheme = dark) {
                Row {
                    Box(Modifier.width(420.dp).fillMaxHeight()) {
                        PrivacyPolicyContent(onBack = {})
                    }
                    Spacer(Modifier.width(40.dp))
                    Box(Modifier.width(420.dp).fillMaxHeight()) {
                        AboutContent(onBack = {}, onNavigateToPrivacy = {}, onNavigateToLicenses = {})
                    }
                }
            }
        }
        rule.onNodeWithText("Cayboard").assertIsDisplayed()
        rule.onNodeWithText("Cayboard 隐私政策").assertIsDisplayed()
        rule.onNodeWithText("更新内容").assertDoesNotExist()
        rule.onNodeWithText("鸣谢").assertDoesNotExist()
        rule.onNodeWithText("构建:", substring = true).assertDoesNotExist()
        val root = rule.activity.window.decorView
        val image = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        rule.runOnIdle { root.draw(android.graphics.Canvas(image)) }
        val directory = File(requireNotNull(out)).apply { mkdirs() }
        val mode = if (dark) "dark" else "light"
        File(directory, "cayboard-about-privacy-$mode.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
