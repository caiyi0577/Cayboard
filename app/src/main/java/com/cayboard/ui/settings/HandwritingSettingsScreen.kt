package com.cayboard.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.twotone.Inventory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * 手写的设置都放在这一页（用户 10-21 要求和语音分开）：停顿识别、公式标记、手写模型。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwritingSettingsContent(
    onBack: () -> Unit,
    onOpenModels: () -> Unit = {},
) {
    Scaffold(modifier = Modifier.testTag("handwriting-settings"), containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(title = { Text("手写") }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).settingsVerticalScroll().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HandwritingSettings()
            SettingsSection(title = "手写模型", content = {
                SettingsItem(Icons.TwoTone.Inventory, "已安装的手写模型",
                    "内置手写与公式识别模型，查看名称、版本和占用空间。", onClick = onOpenModels, showArrow = true)
            })
        }
    }
}
