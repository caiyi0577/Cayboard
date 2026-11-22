package com.cayboard.ui.render

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.ui.keyboard.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h600dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KeyboardGlyphCenterTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun paintedPunctuationCentersInBothAxes() {
        AppFonts.initialize(ApplicationProvider.getApplicationContext())
        val symbols = listOf("，", "。", "？", "！", "、", "：", "；", "“", "”", "（", "）", "【", "】", "…", "—")
        rule.setContent { MaterialTheme {
            Column {
                symbols.chunked(5).forEach { row -> Row {
                    row.forEach { symbol ->
                        Box(Modifier.size(64.dp).background(Color.Black).testTag("ink:$symbol"), contentAlignment = Alignment.Center) {
                            KeyboardGlyphText(symbol, 24.sp, Color.White)
                        }
                    }
                } }
            }
        } }
        rule.waitForIdle()
        val root = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        rule.runOnIdle { root.draw(android.graphics.Canvas(bitmap)) }
        val errors = mutableListOf<String>()
        for (symbol in symbols) {
            val box = rule.onNodeWithTag("ink:$symbol", true).fetchSemanticsNode().boundsInRoot
            var left = Int.MAX_VALUE; var top = Int.MAX_VALUE; var right = -1; var bottom = -1
            for (y in box.top.toInt() until box.bottom.toInt()) for (x in box.left.toInt() until box.right.toInt()) {
                val pixel = bitmap.getPixel(x,y)
                if (android.graphics.Color.red(pixel) > 128 && android.graphics.Color.green(pixel) > 128) {
                    left = minOf(left,x); right = maxOf(right,x); top = minOf(top,y); bottom = maxOf(bottom,y)
                }
            }
            assertTrue("$symbol has visible ink", right >= left && bottom >= top)
            val dx = (left+right+1)/2f - box.center.x
            val dy = (top+bottom+1)/2f - box.center.y
            if (kotlin.math.abs(dx) > 2f || kotlin.math.abs(dy) > 2f) errors += "$symbol offset ($dx, $dy)"

        }
        bitmap.recycle()
        assertTrue(errors.joinToString("; "), errors.isEmpty())
        assertFalse(isKeyboardGlyphLabel("chua"))
        assertFalse(isKeyboardGlyphLabel("分词"))
    }
}
