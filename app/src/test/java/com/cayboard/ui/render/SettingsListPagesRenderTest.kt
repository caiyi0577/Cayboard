package com.cayboard.ui.render

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.CustomLayoutManager
import com.cayboard.settings.CellDictionaryState
import com.cayboard.settings.InputModes
import com.cayboard.settings.InstalledCellDictionary
import com.cayboard.settings.SchemaMeta
import com.cayboard.settings.SettingsPreferences
import com.cayboard.ui.settings.LayoutDisplaySettingsContent
import com.cayboard.ui.settings.SchemaToggleItem
import com.cayboard.ui.settings.SettingsGroupTitle
import com.cayboard.ui.settings.SettingsNote
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
 * 统一样式后的三张列表页放在一起看：「导入词库」、布局管理、已安装的方案（已启用的组合一张卡片），
 * 再加「键盘布局」页看新图标。浅色、深色各一张。只在设置了 CYIME_RENDER_OUT 时出图。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w1840dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsListPagesRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun renderListPages() {
        val out = System.getenv("CYIME_RENDER_OUT")
        assumeTrue(!out.isNullOrEmpty())
        val context = ApplicationProvider.getApplicationContext<Context>()
        KeyboardThemes.initFromConfig(context)
        val dark = mutableStateOf(false)
        val schemas = listOf(SchemaMeta("rime_ice", "中文26键"), SchemaMeta("t9_pinyin", "中文九键", "3.0.0", "Dvel"),
            SchemaMeta(InputModes.ENGLISH, "英文"))
        rule.setContent {
            CayboardTheme(darkTheme = dark.value) {
                Row {
                    Box(Modifier.width(420.dp).fillMaxHeight()) {
                        CustomLayoutManager(SettingsPreferences.getKeyboardTheme(context), dark.value) {}
                    }
                    Spacer(Modifier.width(20.dp))
                    Surface(Modifier.width(420.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            SettingsGroupTitle("已启用的输入组合", Modifier.padding(top = 8.dp))
                            schemas.forEachIndexed { index, schema ->
                                val english = schema.schemaId == InputModes.ENGLISH
                                SchemaToggleItem(schema, enabled = true, isCompiled = true, isCurrent = index == 0,
                                    onToggle = {}, onSelect = {}, isBuiltIn = english,
                                    groupIndex = index, groupSize = schemas.size)
                            }
                            SettingsNote("长按可排序；语言和默认组合在「语言与方案」选择。")
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Box(Modifier.width(420.dp).fillMaxHeight()) { LayoutDisplaySettingsContent(onBack = {}) }
                }
            }
        }
        File(out!!).mkdirs()
        for (theme in listOf("light", "dark")) {
            rule.runOnIdle { dark.value = theme == "dark" }
            rule.waitForIdle()
            val root = rule.activity.window.decorView
            val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            rule.runOnIdle { root.draw(android.graphics.Canvas(bitmap)) }
            File(out, "settings-list-pages-$theme.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
