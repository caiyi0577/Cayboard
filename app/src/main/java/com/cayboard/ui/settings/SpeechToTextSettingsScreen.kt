package com.cayboard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.GraphicEq
import androidx.compose.material.icons.twotone.DirectionsCar
import androidx.compose.material.icons.twotone.Computer
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.twotone.VolumeOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import com.cayboard.speech.VoiceOtherAudio
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cayboard.speech.AsrBackendFactory
import com.cayboard.settings.SettingsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 「屏蔽外放音」一行：右边写当前选的，点开单选框（样式同首页「界面语言」）。 */
@Composable
private fun VoiceOtherAudioItem() {
    val context = LocalContext.current
    var current by remember { mutableStateOf(SettingsPreferences.getSttOtherAudio(context)) }
    var picking by remember { mutableStateOf(false) }
    SettingsItem(icon = Icons.AutoMirrored.TwoTone.VolumeOff, title = "屏蔽外放音",
        subtitle = "说话时压低或暂停正在放的音乐、视频", status = current.label, onClick = { picking = true })
    if (picking) AlertDialog(
        onDismissRequest = { picking = false },
        title = { Text("屏蔽外放音") },
        text = {
            Column {
                VoiceOtherAudio.entries.forEach { mode ->
                    val choose = {
                        picking = false
                        if (mode != current) { current = mode; SettingsPreferences.setSttOtherAudio(context, mode) }
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = choose)
                        .testTag("voice-other-audio:${mode.id}"), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = mode == current, onClick = choose)
                        Spacer(Modifier.width(8.dp))
                        Text(mode.label, style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (mode == current) FontWeight.Medium else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { picking = false }) { Text("关闭") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechToTextSettingsContent(
    onBack: () -> Unit,
) {
    val context = LocalContext.current


    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("语音转文本", style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .settingsVerticalScroll()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 语音总开关放在页内最上面（以前挂在首页那一行上，进来反而找不到）。
            var sttEnabled by remember { mutableStateOf(SettingsPreferences.isSttEnabled(context)) }
            SettingsSection(title = "语音输入") {
                SettingsToggleItem(icon = Icons.TwoTone.GraphicEq, title = "语音转文本",
                    subtitle = if (sttEnabled) "点工具栏麦克风说话，转成文字上屏；想按住空格说话，在「按键与手势 › 长按空格」选「语音开关」"
                        else "已关闭：键盘上不显示麦克风，按住空格和 Alt + H 不开麦",
                    subtitleJumps = listOf("按键与手势 › 长按空格"),
                    checked = sttEnabled, onCheckedChange = { sttEnabled = it; SettingsPreferences.setSttEnabled(context, it) })
                // 开车连续语音：别人提的需求，默认关（VoicePillResume）。只在语音打开时生效，关着语音就不显示。
                if (sttEnabled) {
                    // 屏蔽外放音：用户 11-04 要的，选项照他的截图，默认不屏蔽（VoiceAudioFocus）。
                    VoiceOtherAudioItem()
                    var pillResume by remember { mutableStateOf(SettingsPreferences.isSttPillResume(context)) }
                    SettingsToggleItem(icon = Icons.TwoTone.DirectionsCar, title = "开车连续语音",
                        subtitle = "语音收起成悬浮条后，发送完回到输入框自动接着听",
                        checked = pillResume, onCheckedChange = { pillResume = it; SettingsPreferences.setSttPillResume(context, it) })
                }
            }

            SettingsSection(title = "语音标点") { SpeechPunctuationSettings() }

            SettingsSection(title = "数字") { SpeechNumberSettings() }

            SettingsSection(title = "语音模型") {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("X-ASR INT8", style = MaterialTheme.typography.titleMedium)
                    Text("版本：2026-06-03", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
