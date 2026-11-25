package com.cayboard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cayboard.model.*
import com.cayboard.speech.AsrModelManager
import com.cayboard.speech.SpeechModelCatalog
import com.cayboard.speech.SpeechModelSelection
import com.cayboard.speech.SpeechLanguages
import com.cayboard.settings.InputLanguage
import com.cayboard.settings.SettingsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun SpeechPunctuationSettings() {
    val context = LocalContext.current
    var autoPunctuation by remember { mutableStateOf(SettingsPreferences.isSttAutoPunctuation(context)) }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    Row(Modifier.fillMaxWidth().settingsSearchAnchor("自动加标点").testTag("speech-auto-punctuation")
        .toggleable(value = autoPunctuation, role = Role.Switch,
            onValueChange = { autoPunctuation = it; SettingsPreferences.setSttAutoPunctuation(context, it) })
        .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("自动加标点", style = MaterialTheme.typography.bodyLarge)
            Text("保留识别结果中的标点。关闭后停顿留空格，口述的标点照常输入。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = autoPunctuation, onCheckedChange = null,
            colors = settingsSwitchColors())
    }
    // 只带一部分标点：自动加标点开着时逐项一行开关，默认全开；关掉的那类在停顿处留空格。
    if (autoPunctuation) listOf(
        Triple("逗号、顿号、分号", SettingsPreferences::isSttPunctuationComma, SettingsPreferences::setSttPunctuationComma),
        Triple("句号", SettingsPreferences::isSttPunctuationPeriod, SettingsPreferences::setSttPunctuationPeriod),
        Triple("问号、感叹号", SettingsPreferences::isSttPunctuationQuestion, SettingsPreferences::setSttPunctuationQuestion),
        Triple("末尾的标点", SettingsPreferences::isSttPunctuationTrailing, SettingsPreferences::setSttPunctuationTrailing),
    ).forEach { (title, read, write) ->
        var kept by remember(title) { mutableStateOf(read(context)) }
        HorizontalDivider(modifier = Modifier.padding(start = 32.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Row(Modifier.fillMaxWidth().testTag("speech-auto-punctuation:$title")
            .toggleable(value = kept, role = Role.Switch, onValueChange = { kept = it; write(context, it) })
            .padding(start = 32.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(when {
                    title == "末尾的标点" -> if (kept) "整段话最后保留句号、问号等标点。" else "整段话最后不带标点，中间的照留；口述照样出。"
                    kept -> "保留模型打的$title。"
                    else -> "不带$title，停顿处留空格；口述照样出。"
                },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = kept, onCheckedChange = null, colors = settingsSwitchColors())
        }
    }
}

/** 数字转阿拉伯数字：别人提的需求，默认关(VoiceNumbers)。 */
@Composable
internal fun SpeechNumberSettings() {
    val context = LocalContext.current
    var arabic by remember { mutableStateOf(SettingsPreferences.isSttArabicNumbers(context)) }
    Row(Modifier.fillMaxWidth().settingsSearchAnchor("数字转阿拉伯数字").testTag("speech-arabic-numbers")
        .toggleable(value = arabic, role = Role.Switch,
            onValueChange = { arabic = it; SettingsPreferences.setSttArabicNumbers(context, it) })
        .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("数字转阿拉伯数字", style = MaterialTheme.typography.bodyLarge)
            Text("中文说的数字写成 325、3.5、50%；一个、第一、一心一意这类不变。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = arabic, onCheckedChange = null, colors = settingsSwitchColors())
    }
}

