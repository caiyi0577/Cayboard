package com.cayboard.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.twotone.MenuBook
import androidx.compose.material.icons.twotone.Draw
import androidx.compose.material.icons.twotone.Swipe
import androidx.compose.material.icons.twotone.Language
import androidx.compose.material.icons.twotone.AppSettingsAlt
import androidx.compose.material.icons.twotone.Translate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Speed
import androidx.compose.material.icons.twotone.AutoAwesome
import androidx.compose.material.icons.twotone.Backup
import androidx.compose.material.icons.twotone.Memory

import androidx.compose.material.icons.twotone.Build
import androidx.compose.material.icons.twotone.Description
import androidx.compose.material.icons.twotone.Extension
import androidx.compose.material.icons.twotone.GraphicEq
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Keyboard
import androidx.compose.material.icons.twotone.KeyboardAlt
import androidx.compose.material.icons.twotone.Palette
import androidx.compose.material.icons.twotone.QueryStats
import androidx.compose.material.icons.twotone.Storefront
import androidx.compose.material.icons.twotone.Straighten
import androidx.compose.material.icons.twotone.Sync
import androidx.compose.material.icons.twotone.TableChart
import androidx.compose.material.icons.twotone.ToggleOn
import androidx.compose.material.icons.twotone.TypeSpecimen
import androidx.compose.material.icons.twotone.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.cayboard.R
import com.cayboard.settings.SettingsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SettingsMainContent(
    onNavigateToSchema: () -> Unit,
    onNavigateToLayoutDisplay: () -> Unit,
    onNavigateToModelLocal: () -> Unit = {},
    onNavigateToSmartPrediction: () -> Unit,
    onNavigateToSpeechToText: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToClipboard: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToHardwareKeyboard: () -> Unit = {},
    onNavigateToLanguages: () -> Unit = {},
    onNavigateToRoute: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var searchQuery by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var entryStatus by remember { mutableStateOf(readMainEntryStatus(context)) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        // 系统输入法选择框关闭、插拔键盘都不一定触发 resume，停留在本页时定时刷新。
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while (true) { entryStatus = readMainEntryStatus(context); kotlinx.coroutines.delay(500) }
        }
    }
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val imeBottomDp = with(density) { imeBottomPx.toDp() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = Color.Unspecified
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.settingsGutterScroll(listState, scrollBehavior.nestedScrollConnection)
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .consumeWindowInsets(innerPadding)
                .padding(horizontal = 16.dp),
            // Keep drawing behind the transparent IME gutters. IME space belongs to
            // scroll content, not a clipped full-width viewport below the toolbar.
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = maxOf(innerPadding.calculateBottomPadding(), imeBottomDp) + SETTINGS_BOTTOM_SPACE,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SettingsSearchField(searchQuery, { searchQuery = it }) }
                    if (searchQuery.isNotBlank()) item {
                SettingsSearchResults(searchQuery) { entry ->
                    if (entry.target == SEARCH_TARGET_CUSTOM_LAYOUT) {
                        context.startActivity(Intent(context, com.cayboard.CustomLayoutActivity::class.java))
                    } else onNavigateToRoute(entry.target)
                }
            }
            item {
                SettingsSection(title = "", searchKey = "设置", content = {
                    SettingsItem(
                        icon = Icons.TwoTone.AppSettingsAlt,
                        title = stringResource(R.string.item_enable_ime_title), searchKey = "启用输入法",
                        subtitle = stringResource(R.string.item_enable_ime_summary),
                        status = if (entryStatus.imeEnabled) stringResource(R.string.status_set) else stringResource(R.string.status_not_set),
                        statusTone = if (entryStatus.imeEnabled) SettingsStatusTone.OK else SettingsStatusTone.WARNING,
                        onClick = {
                            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        icon = Icons.TwoTone.ToggleOn,
                        title = stringResource(R.string.item_select_ime_title), searchKey = "选择输入法",
                        subtitle = stringResource(R.string.item_select_ime_summary),
                        status = if (entryStatus.imeSelected) stringResource(R.string.status_set) else stringResource(R.string.status_not_set),
                        statusTone = if (entryStatus.imeSelected) SettingsStatusTone.OK else SettingsStatusTone.WARNING,
                        onClick = {
                            val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) 
                                as InputMethodManager
                            imm.showInputMethodPicker()
                        }
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SettingsItem(icon = Icons.TwoTone.Language, title = stringResource(R.string.item_languages_title), searchKey = "语言与方案",
                        subtitle = stringResource(R.string.item_languages_summary), onClick = onNavigateToLanguages, showArrow = true)
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        icon = Icons.TwoTone.Keyboard,
                        title = stringResource(R.string.item_layouts_title), searchKey = "键盘布局",
                        subtitle = stringResource(R.string.item_layouts_summary),
                        onClick = onNavigateToLayoutDisplay,
                        showArrow = true
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        icon = Icons.TwoTone.Swipe,
                        title = stringResource(R.string.item_gestures_title), searchKey = "按键与手势",
                        subtitle = stringResource(R.string.item_gestures_summary),
                        onClick = { onNavigateToRoute("${SettingsRoutes.LayoutDisplay}?page=gestures") },
                        showArrow = true
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(icon = Icons.TwoTone.KeyboardAlt, title = stringResource(R.string.item_hardware_title), searchKey = "物理键盘",
                        subtitle = stringResource(R.string.item_hardware_summary), onClick = onNavigateToHardwareKeyboard,
                        status = if (entryStatus.keyboardConnected) stringResource(R.string.status_connected) else stringResource(R.string.status_not_connected),
                        statusTone = if (entryStatus.keyboardConnected) SettingsStatusTone.OK else SettingsStatusTone.NEUTRAL)
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    EmojiStyleSetting()
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    SettingsItem(
                        icon = Icons.TwoTone.TableChart,
                        title = stringResource(R.string.item_candidates_title), searchKey = "候选词",
                        subtitle = stringResource(R.string.item_candidates_summary),
                        onClick = { onNavigateToRoute("${SettingsRoutes.LayoutDisplay}?page=candidates") },
                        showArrow = true
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SettingsItem(Icons.TwoTone.Translate, "翻译", "内置模型、多语种互译",
                        onClick = { onNavigateToRoute(SettingsRoutes.Translation) }, showArrow = true)
                    HorizontalDivider(Modifier.padding(start = 56.dp), thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SettingsItem(Icons.TwoTone.Memory, "模型管理", "查看所有离线模型的版本和容量",
                        onClick = onNavigateToModelLocal, showArrow = true)
                    HorizontalDivider(Modifier.padding(start = 56.dp), thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SettingsItem(
                        icon = Icons.TwoTone.AutoAwesome,
                        title = stringResource(R.string.item_prediction_title), searchKey = "智能联想",
                        subtitle = stringResource(R.string.item_prediction_summary),
                        onClick = onNavigateToSmartPrediction,
                        showArrow = true
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    // 首页这一行右边就是语音开关，点开关直接开关语音，点行进语音页（用户 10-20 要求恢复）。
                    var sttEnabled by remember(entryStatus.sttEnabled) { mutableStateOf(entryStatus.sttEnabled) }
                    SettingsToggleItem(
                        icon = Icons.TwoTone.GraphicEq,
                        title = stringResource(R.string.item_speech_title), searchKey = "语音转文本",
                        subtitle = stringResource(R.string.item_speech_summary),
                        checked = sttEnabled,
                        showArrow = true,
                        onClick = onNavigateToSpeechToText,
                        onCheckedChange = { enabled ->
                            sttEnabled = enabled
                            SettingsPreferences.setSttEnabled(context, enabled)
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        icon = Icons.TwoTone.Draw,
                        title = stringResource(R.string.item_handwriting_title), searchKey = "手写",
                        subtitle = stringResource(R.string.item_handwriting_summary),
                        onClick = { onNavigateToRoute(SettingsRoutes.Handwriting) },
                        showArrow = true
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SettingsItem(
                        icon = Icons.TwoTone.Sync,
                        title = stringResource(R.string.item_clipboard_title), searchKey = "剪贴板",
                        subtitle = stringResource(R.string.item_clipboard_summary),
                        onClick = onNavigateToClipboard,
                        showArrow = true
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        icon = Icons.TwoTone.Backup,
                        title = stringResource(R.string.item_backup_title), searchKey = "备份与恢复",
                        subtitle = stringResource(R.string.item_backup_summary),
                        onClick = onNavigateToBackup,
                        showArrow = true
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        icon = Icons.TwoTone.Info,
                        title = stringResource(R.string.item_about_title), searchKey = "关于 Cayboard",
                        subtitle = stringResource(R.string.item_about_summary),
                        onClick = onNavigateToAbout,
                        showArrow = true
                    )
                })
            }
        }
    }
}

/** 首页入口右侧的状态：输入法启用/选中、实体键盘连接、当前主题。 */
internal data class MainEntryStatus(
    val imeEnabled: Boolean = false,
    val imeSelected: Boolean = false,
    val keyboardConnected: Boolean = false,
    val themeName: String = "",
    val sttEnabled: Boolean = false,
)

internal fun readMainEntryStatus(context: android.content.Context): MainEntryStatus {
    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val input = context.getSystemService(android.content.Context.INPUT_SERVICE) as? android.hardware.input.InputManager
    val devices: List<android.view.InputDevice> =
        input?.let { manager -> manager.inputDeviceIds.map { id -> manager.getInputDevice(id) }.filterNotNull() }.orEmpty()
    val theme = com.cayboard.ui.theme.KeyboardThemes.getThemeById(SettingsPreferences.getKeyboardTheme(context)).name
    return MainEntryStatus(
        imeEnabled = imm?.enabledInputMethodList?.any { it.packageName == context.packageName } == true,
        imeSelected = isCayboardSelected(context),
        keyboardConnected = com.cayboard.service.hasPhysicalTextKeyboard(devices),
        themeName = theme,
        sttEnabled = SettingsPreferences.isSttEnabled(context),
    )
}

/** 下载提示里的大小：和模型管理里写的 MB 一样按 1024 × 1024 字节算，取整。 */
internal fun formatModelMegabytes(bytes: Long): String = "${(bytes + 524_288) / 1_048_576} MB"


