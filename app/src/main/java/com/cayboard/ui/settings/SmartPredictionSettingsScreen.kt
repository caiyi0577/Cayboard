package com.cayboard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cayboard.settings.SettingsPreferences
import com.cayboard.viewmodel.SmartPredictionSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPredictionSettingsContent(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: SmartPredictionSettingsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            viewModel.showToast(message)
            viewModel.clearToast()
        }
    }

    KeyboardSettingsScaffold("智能联想", onBack, preview = true,
        example = if (uiState.isEnabled) com.cayboard.keyboard.KeyboardPanelLaunch.Example.Associations
            else com.cayboard.keyboard.KeyboardPanelLaunch.Example.Keyboard) { modifier ->
        val listState = rememberSettingsSearchListState()
        LazyColumn(
            state = listState,
            modifier = modifier.settingsGutterScroll(listState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = keyboardPreviewContentPadding(top = 8.dp, bottom = 8.dp)
        ) {
            item {
                SettingsSection(title = "功能开关", content = {
                    Row(
                        modifier = Modifier.settingsSearchAnchor("启用智能联想")
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "启用智能联想",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "上屏后接着给出下一个词",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PerformanceCostNote(com.cayboard.settings.PerformanceCosts.Item.PREDICTION)
                        }
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Switch(
                                checked = uiState.isEnabled,
                                onCheckedChange = { viewModel.setEnabled(it) },
                                colors = SwitchDefaults.colors()
                            )
                        }
                    }
                })
            }

            if (uiState.isEnabled) {
                item {
                    var hardwareAssociationDisabled by remember {
                        mutableStateOf(SettingsPreferences.isHardwareAssociationDisabled(context))
                    }
                    var spaceCommitEnabled by remember {
                        mutableStateOf(SettingsPreferences.isSpaceCommitAssociationEnabled(context))
                    }
                    var singleMode by remember {
                        mutableStateOf(SettingsPreferences.isSingleAssociationMode(context))
                    }
                    Column { SettingsSection(title = "联想行为", content = {
                        SettingsToggleItem(
                            icon = lucideIcon(LucideSettingIcons.KEYBOARD),
                            title = "物理键盘关闭联想",
                            subtitle = "连接物理键盘时，文字上屏后不再联想下一个词",
                            checked = hardwareAssociationDisabled,
                            onCheckedChange = {
                                hardwareAssociationDisabled = it
                                SettingsPreferences.setHardwareAssociationDisabled(context, it)
                            },
                        )
                        HorizontalDivider()
                        SettingsToggleItem(
                            icon = lucideIcon(LucideSettingIcons.SPARKLES),
                            title = "空格上屏联想候选",
                            subtitle = "有联想词时，按空格上屏第一个",
                            checked = spaceCommitEnabled,
                            onCheckedChange = {
                                spaceCommitEnabled = it
                                SettingsPreferences.setSpaceCommitAssociationEnabled(context, it)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        AssociationModeRow(
                            title = "连续联想",
                            subtitle = "选了联想词后接着出下一批，接不上时自动停",
                            isSelected = !singleMode,
                            onClick = {
                                singleMode = false
                                SettingsPreferences.setAssociationSingleMode(context, false)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        AssociationModeRow(
                            title = "单次联想",
                            subtitle = "选一个联想词就停，下次打字再出",
                            isSelected = singleMode,
                            onClick = {
                                singleMode = true
                                SettingsPreferences.setAssociationSingleMode(context, true)
                            }
                        )
                    })
                    // 「退格直接删字」「删字后给同音字」11-01 搬到 按键与手势 › 删除键，这里只指个路，不放第二个开关。
                    SettingsNote("退格直接删字、删字后给同音字在「按键与手势 › 删除键」。", jumps = listOf("按键与手势 › 删除键"))
                    }
                }
            }

        }
    }
}

@Composable
private fun AssociationModeRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.settingsSearchAnchor(title)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(selected = isSelected, onClick = onClick)
    }
}

