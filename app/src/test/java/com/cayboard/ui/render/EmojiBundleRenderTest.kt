package com.cayboard.ui.render

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.data.EmojiData
import com.cayboard.settings.SettingsPreferences
import com.cayboard.ui.keyboard.*
import com.cayboard.ui.settings.EmojiStyleSetting
import com.cayboard.ui.theme.CayboardTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "zh-rCN-w411dp-h620dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EmojiBundleRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val app get() = ApplicationProvider.getApplicationContext<Application>()

    @Test fun catalogFontAndChineseKeywordsAreComplete() {
        val expected = app.assets.open("emoji/catalog.tsv").bufferedReader().useLines { rows ->
            rows.map { it.substringAfter('\t') }.toSet()
        }
        assertEquals(3972, expected.size)
        assertEquals(expected, EmojiData.all)
        val paint = Paint().apply {
            typeface = Typeface.createFromAsset(app.assets, "emoji/Android.ttf"); textSize = 48f
        }
        val missing = expected.filterNot { paint.hasGlyph(it) }
        assertTrue("Missing Android glyphs: $missing", missing.isEmpty())
        val keywords = app.assets.open("cayboard_chinese/opencc/emoji.txt").bufferedReader().useLines { rows ->
            rows.associate { it.substringBefore('\t') to it.substringAfter('\t').split(' ') }
        }
        assertEquals(listOf("喷泉", "⛲"), keywords["喷泉"])
        assertTrue(keywords.getValue("咖啡").contains("☕"))
        assertTrue(keywords.getValue("开心").contains("😄"))
    }

    @Test fun candidateDefaultMigrationOnlyRunsOnceAndDoesNotEraseWords() {
        val prefs = SettingsPreferences.getPrefsPublic(app)
        prefs.edit().clear().putString("unrelated_user_phrase", "我的词").putBoolean("candidate_emoji",false).commit()
        EmojiAppearance.migrateCandidates(app)
        assertTrue(KeyboardInputPreferences.read(app).candidateEmoji)
        prefs.edit().putBoolean("candidate_emoji",false).commit()
        EmojiAppearance.migrateCandidates(app)
        assertFalse(KeyboardInputPreferences.read(app).candidateEmoji)
        assertEquals("我的词",prefs.getString("unrelated_user_phrase",null))
    }

    @Test fun bothStylesRenderInPanelAndCandidatesAndSwitchImmediately() {
        AppFonts.initialize(app)
        val emojis = listOf("😀", "⛲", "❤️", "☕", "👍🏽", "👩‍💻", "👨‍👩‍👧‍👦", "🏳️‍🌈", "🫩", "🫪")
        rule.setContent {
            CayboardTheme(darkTheme = true) {
                Surface {
                Column(Modifier.fillMaxSize().background(Color(0xFF202020)).padding(12.dp)) {
                    EmojiStyleSetting()
                    Text("${EmojiAppearance.style.name} · 表情与候选", color=Color.White)
                    emojis.chunked(5).forEach { row ->
                        Row {
                            row.forEach { emoji -> EmojiButton(emoji, {}, Modifier.size(72.dp), 72f) }
                        }
                    }
                    Text(EmojiAppearance.annotate("喷泉 ⛲ · 开心 😄 · 咖啡 ☕"),fontSize=24.sp,color=Color.White)
                    CandidateItem("1. ⛲",0,{},Color.White,isSelected=true)
                    Text("长按肤色示例",color=Color.White)
                    Row { listOf("👍", "👍🏻", "👍🏼", "👍🏽", "👍🏾", "👍🏿").forEach {
                        EmojiButton(it,{},Modifier.size(56.dp),56f)
                    } }
                }
                }
            }
        }
        val frames = mutableListOf<Bitmap>()
        EmojiAppearance.Style.entries.forEach { style ->
            rule.runOnIdle { EmojiAppearance.setStyle(app,style) }
            rule.waitForIdle()
            assertEquals(style.name, SettingsPreferences.getPrefsPublic(app).getString(EmojiAppearance.KEY,null))
            val spans=EmojiAppearance.annotate("1. ⛲")
            assertEquals("1. ⛲",spans.text)
            assertEquals(3,spans.spanStyles.single().start)
            val root=rule.activity.window.decorView
            val bitmap=Bitmap.createBitmap(root.width,root.height,Bitmap.Config.ARGB_8888)
            rule.runOnIdle { root.draw(android.graphics.Canvas(bitmap)) }
            frames += bitmap
            System.getenv("CAYBOARD_RENDER_OUT")?.let { dir ->
                File(dir).mkdirs()
                File(dir,"emoji-${style.name}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            }
        }
        assertFalse("Styles must visibly differ",frames[0].sameAs(frames[1]))
        // Compare the glyph grid itself; a changed style label alone is insufficient.
        val androidPixels = IntArray(700*300)
        val iosPixels = IntArray(androidPixels.size)
        frames[0].getPixels(androidPixels,0,700,24,240,700,300)
        frames[1].getPixels(iosPixels,0,700,24,240,700,300)
        assertTrue(androidPixels.indices.count { androidPixels[it] != iosPixels[it] } > 5000)
        // The actual settings dialog exposes the exact requested option names.
        rule.onNodeWithText("Emoji 显示样式").performClick()
        rule.onNodeWithTag("settings-search-anchor:Android").assertExists()
        rule.onNodeWithTag("settings-search-anchor:iOS").assertExists()
        rule.onNodeWithTag("settings-search-anchor:Android").performClick()
        rule.waitForIdle()
        assertEquals(EmojiAppearance.Style.Android,EmojiAppearance.style)
    }
}
