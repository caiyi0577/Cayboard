package com.cayboard.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.cayboard.translate.TranslationLanguages
import com.cayboard.translate.BundledTranslationModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cayboard.translate.OfflineTranslation
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationSettingsContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { context.getSharedPreferences("translation", android.content.Context.MODE_PRIVATE) }
    var source by remember { mutableStateOf(store.getString("sourceLang", "自动检测") ?: "自动检测") }
    var target by remember { mutableStateOf(store.getString("targetLang", "英文") ?: "英文") }
    var choosing by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("翻译") }, navigationIcon = {
        IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回") }
    }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("translation-settings"),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = SETTINGS_BOTTOM_SPACE),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item { SettingsNote("多语种离线翻译，模型随输入法内置，无需下载。在键盘菜单点「翻译」，输入后停顿即可生成译文。") }
            item { SettingsSection("语言") {
                SettingsItem(lucideIcon(LucideSettingIcons.LANGUAGES),"原文语言",source,onClick={choosing="source"},showArrow=true)
                SettingsItem(lucideIcon(LucideSettingIcons.LANGUAGES),"翻译成",target,onClick={choosing="target"},showArrow=true)
            } }
            item { SettingsSection("离线模型") {
                SettingsListRow(BundledTranslationModel.NAME,
                    "版本：${BundledTranslationModel.VERSION} · 大小：${BundledTranslationModel.SIZE_LABEL}",
                    modifier=Modifier.testTag("translation-model-details"))
            } }
        }
    }
    choosing?.let { key ->
        val options=if(key=="source") listOf("自动检测") + TranslationLanguages.all.map {it.name} else TranslationLanguages.all.map {it.name}
        AlertDialog(onDismissRequest={choosing=null},title={Text(if(key=="source") "原文语言" else "翻译成")},
            text={ LazyColumn(Modifier.heightIn(max=420.dp)) { items(options) { value ->
                TextButton(onClick={
                    if(key=="source") {
                        source=value
                        if(source==target) {target=if(source=="英文") "简体中文" else "英文";store.edit().putString("targetLang",target).apply()}
                        store.edit().putString("sourceLang",source).apply()
                    } else {
                        target=value
                        if(source==target) {source=if(target=="英文") "简体中文" else "英文";store.edit().putString("sourceLang",source).apply()}
                        store.edit().putString("targetLang",target).apply()
                    }
                    choosing=null
                },modifier=Modifier.fillMaxWidth()) {Text(value,modifier=Modifier.fillMaxWidth())}
            } } },confirmButton={TextButton(onClick={choosing=null}) {Text("取消")}})
    }
}

