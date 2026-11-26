package com.cayboard.ui.render

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.ui.keyboard.*
import com.cayboard.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h300dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DarkKeycapDepthRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun deleteSwipeFeedbackFollowsKeycapCornersInBothAppearances() {
        AppFonts.initialize(rule.activity)
        var dark by mutableStateOf(true)
        var cleared = 0
        var undone = 0
        val surround = Color(0xFF0B0D10)
        rule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            val palette = resolveKeyboardPalette(FixedKeyboardStyle.ID, dark)
            CompositionLocalProvider(LocalConfiguration provides configuration, LocalKeyboardPalette provides palette) {
                CayboardTheme(darkTheme = dark) {
                    Box(Modifier.fillMaxSize().background(surround), contentAlignment = Alignment.Center) {
                        SwipeableIconKeyButton(
                            rememberVectorPainter(Icons.AutoMirrored.Filled.Backspace), {},
                            palette.function, palette.functionText, Modifier.size(76.dp, 56.dp).testTag("delete-corners"),
                            swipeUpLabel = "上滑清空", swipeDownLabel = "下滑撤回",
                            onSwipeUp = { cleared++ }, onSwipeDown = { undone++ },
                            upFeedback = KeyFlickFeedback.DELETE, downFeedback = KeyFlickFeedback.UNDO,
                            visualPadding = PaddingValues(0.dp), shadowEnabled = false, deleteKey = true,
                        )
                    }
                }
            }
        }
        val key = rule.onNodeWithTag("delete-corners", true)
        fun snapshot(): Bitmap {
            val bounds = key.fetchSemanticsNode().boundsInRoot
            val view = rule.activity.window.decorView
            val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            rule.runOnIdle { view.draw(android.graphics.Canvas(full)) }
            return Bitmap.createBitmap(full, bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt())
        }
        fun checkCorners(image: Bitmap) {
            for (x in listOf(0, image.width - 1)) for (y in listOf(0, image.height - 1)) {
                assertEquals("Corner ($x, $y) should expose the background", surround.toArgb(), image.getPixel(x, y))
            }
        }
        for (night in listOf(true, false)) {
            rule.runOnIdle { dark = night }
            val idle = snapshot()
            checkCorners(idle)
            for (direction in listOf(-1f, 1f)) {
                key.performTouchInput { down(center); moveTo(center + Offset(0f, center.y * 3.3f * direction)) }
                val feedback = snapshot()
                checkCorners(feedback)
                val color = feedback.getPixel(feedback.width / 2, feedback.height / 5)
                assertNotEquals(idle.getPixel(idle.width / 2, idle.height / 5), color)
                key.performTouchInput { up() }
                val out = File(System.getenv("CAYBOARD_RENDER_OUT") ?: "build/fixed-keyboard-renders").apply { mkdirs() }
                File(out, "delete-${if (night) "dark" else "light"}-${if (direction < 0) "up" else "down"}.png")
                    .outputStream().use { feedback.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        rule.runOnIdle { assertEquals(2, cleared); assertEquals(2, undone) }
    }

    @Test fun pressMovesOnlyPaintAndRetainsFixedHitBounds() {
        AppFonts.initialize(rule.activity)
        var dark by mutableStateOf(true)
        var pressed by mutableStateOf(false)
        rule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            val palette = resolveKeyboardPalette(FixedKeyboardStyle.ID, dark)
            CompositionLocalProvider(LocalConfiguration provides configuration, LocalKeyboardPalette provides palette) {
                CayboardTheme(darkTheme = dark) {
                    Row(Modifier.requiredSize(360.dp, 84.dp).background(palette.background)
                        .padding(8.dp).testTag("depth-sample")) {
                        listOf("a" to 1f, "abc" to 1.6f, "空格" to 2.5f, "删除" to 1.3f).forEach { (label, weight) ->
                            Box(Modifier.weight(weight).fillMaxHeight().padding(1.dp).testTag("depth-key:$label")
                                .fixedSkinKeycap(label, pressed), contentAlignment = Alignment.Center) {
                                Text(label, color = fixedSkinContentColor(label), fontSize = 22.sp, fontFamily = AppFonts.keyFontFamily)
                            }
                        }
                    }
                }
            }
        }
        val out = File(System.getenv("CAYBOARD_RENDER_OUT") ?: "build/fixed-keyboard-renders").apply { mkdirs() }
        for (night in listOf(true, false)) {
            rule.runOnIdle { dark = night; pressed = false }
            rule.waitForIdle()
            val bounds = rule.onNodeWithTag("depth-key:a", true).fetchSemanticsNode().boundsInRoot
            fun capture(): Bitmap {
                val crop = rule.onNodeWithTag("depth-sample", true).fetchSemanticsNode().boundsInRoot
                val view = rule.activity.window.decorView
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                rule.runOnIdle { view.draw(android.graphics.Canvas(bitmap)) }
                return Bitmap.createBitmap(bitmap, crop.left.toInt(), crop.top.toInt(), crop.width.toInt(), crop.height.toInt())
            }
            val rest = capture()
            rule.runOnIdle { pressed = true }
            rule.waitForIdle()
            assertEquals(bounds, rule.onNodeWithTag("depth-key:a", true).fetchSemanticsNode().boundsInRoot)
            val held = capture()
            assertFalse("Pressed paint must visibly differ", rest.sameAs(held))
            val sampleBounds = rule.onNodeWithTag("depth-sample", true).fetchSemanticsNode().boundsInRoot
            fun glyphCenter(bitmap: Bitmap): Float {
                var sum = 0L
                var count = 0
                for (y in 4 until bitmap.height - 4) {
                    for (x in (bounds.left - sampleBounds.left).toInt() + 4 until (bounds.right - sampleBounds.left).toInt() - 4) {
                        val pixel = bitmap.getPixel(x, y)
                        val red = android.graphics.Color.red(pixel)
                        val green = android.graphics.Color.green(pixel)
                        val blue = android.graphics.Color.blue(pixel)
                        val isGlyph = if (night) minOf(red, green, blue) > 210 else maxOf(red, green, blue) < 90
                        if (isGlyph) { sum += y; count++ }
                    }
                }
                assertTrue("The glyph must be visible", count > 0)
                return sum.toFloat() / count
            }
            val glyphTravel = glyphCenter(held) - glyphCenter(rest)
            assertTrue("Both appearances must lower the glyph without moving its hit target: $glyphTravel", glyphTravel in 1f..4f)
            val combined = Bitmap.createBitmap(rest.width, rest.height * 2 + 12, Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(combined).apply {
                drawColor(if (night) android.graphics.Color.rgb(30, 30, 30) else android.graphics.Color.rgb(228, 231, 236))
                drawBitmap(rest, 0f, 0f, null)
                drawBitmap(held, 0f, (rest.height + 12).toFloat(), null)
            }
            File(out, "key-depth-${if (night) "dark" else "light"}.png").outputStream().use {
                combined.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
