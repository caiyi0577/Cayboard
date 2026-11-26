package com.cayboard.ui.render

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.rememberScrollState
import com.cayboard.settings.SchemaInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.keyboard.*
import com.cayboard.ui.keyboard.*
import com.cayboard.ui.settings.PreviewChoiceCard
import com.cayboard.ui.settings.ProfileChoiceRow
import com.cayboard.ui.settings.settingsSwitchColors
import com.cayboard.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk=[34],application=Application::class,qualifiers="zh-rCN-w840dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SelectionAuditRenderTest {
    @get:Rule val rule=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun actualControlsUseReadableFixedSelectionColors() {
        AppFonts.initialize(ApplicationProvider.getApplicationContext<Application>())
        for (dark in listOf(false,true)) {
            val s=SelectionStyle.colors(dark)
            fun contrast(a:Color,b:Color):Float { val x=a.luminance(); val y=b.luminance();return (maxOf(x,y)+.05f)/(minOf(x,y)+.05f) }
            assertTrue(contrast(s.fill,s.content)>=4.5f)
            assertTrue(contrast(s.quietFill,s.quietContent)>=4.5f)
        }
        rule.setContent {
            Row(Modifier.fillMaxSize()) {
                for (dark in listOf(false,true)) {
                    val config=Configuration(LocalConfiguration.current).apply {
                        uiMode=(uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if(dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    }
                    CompositionLocalProvider(LocalConfiguration provides config, LocalKeyboardInputPreferences provides KeyboardInputPreferences()) {
                        CayboardTheme(darkTheme=dark) {
                            val c=MaterialTheme.colorScheme
                            Column(Modifier.width(420.dp).fillMaxHeight().background(c.surface).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                                Text(if(dark) "深色选中状态" else "浅色选中状态",color=c.onSurface)
                                Row {
                                    Switch(true,{},colors=settingsSwitchColors()); Switch(false,{},colors=settingsSwitchColors())
                                    RadioButton(true,{}); Checkbox(true,{})
                                }
                                Text("候选栏",color=c.onSurface)
                                Row {
                                    CandidateItem("多少钱",0,{},c.onSurface,isSelected=true,accentColor=c.primary,selectedTextColor=c.primary)
                                    CandidateItem("多收钱",1,{},c.onSurface,accentColor=c.primary,selectedTextColor=c.primary)
                                }
                                Text("方案与布局",color=c.onSurface)
                                ProfileChoiceRow("方案","audit-schemes",listOf("pinyin" to "拼音","xiaohe" to "小鹤双拼","microsoft" to "微软双拼"),"pinyin",
                                    containerColor=c.surfaceContainerLow,contentColor=c.onSurface,accentColor=c.primary,
                                    previews=mapOf("pinyin" to "pin'yin","xiaohe" to "xn'he","microsoft" to "ud'pn"),onSelect={})
                                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                    for (chosen in listOf(true,false)) PreviewChoiceCard(if(chosen) "九键" else "26键",chosen,150.dp,80.dp,
                                        c.surfaceContainerLow,c.onSurface,c.primary,"audit-layout:$chosen",onSelect={}) {
                                        Text(if(chosen) "ABC  DEF\nGHI  JKL\nPQRS TUV" else "Q W E R T Y\n A S D F G\n  Z X C V",color=c.onSurface)
                                    }
                                }
                                Text("快捷菜单",color=c.onSurface)
                                MenuBar(MenuBarState(true,dark,backgroundColor=c.surface,keyBgColor=c.surfaceContainer,keyTextColor=c.onSurface),
                                    listOf(ToolbarAction(ToolbarButtonItem.Builtin(ToolbarButton.SOUND),active=true,onClick={}),
                                        ToolbarAction(ToolbarButtonItem.Builtin(ToolbarButton.VIBRATION),active=false,onClick={})),Modifier.fillMaxWidth().height(145.dp))
                                Text("分类与选项",color=c.onSurface)
                                Row { ExpressionTab("表情",true,c.onSurface,c.primary,onClick={});ExpressionTab("颜文字",false,c.onSurface,c.primary,onClick={}) }
                                OptionMenuCard("输入方式",listOf("九键","26键"),"九键",width=240.dp,onChoose={})
                                Text("语言切换",color=c.onSurface)
                                LanguageMenuCard(listOf(SchemaInfo("t9_pinyin","拼音九键","","",""),SchemaInfo("cayboard_pinyin","拼音26键","","",""),SchemaInfo("english","英文","","","")),
                                    "t9_pinyin",240.dp,130.dp,40.dp,rememberScrollState(),{}, {})
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        System.getenv("CAYBOARD_RENDER_OUT")?.let { out ->
            val root=rule.activity.window.decorView
            val bitmap=Bitmap.createBitmap(root.width,root.height,Bitmap.Config.ARGB_8888)
            rule.runOnIdle {root.draw(android.graphics.Canvas(bitmap))}
            File(out,"selection-audit-light-dark.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
}
