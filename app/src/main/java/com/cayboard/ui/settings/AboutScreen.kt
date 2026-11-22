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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material.icons.twotone.Code
import androidx.compose.material.icons.twotone.Description
import androidx.compose.material.icons.twotone.PersonOutline
import androidx.compose.material.icons.twotone.PrivacyTip
import androidx.compose.material.icons.twotone.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cayboard.BuildConfig
import com.cayboard.R
import com.cayboard.settings.SettingsPreferences
import com.cayboard.ui.theme.VisualStyles
import com.cayboard.util.FileLogger
import com.cayboard.platform.AndroidPlatform

data class LicenseItem(
    val name: String,
    val license: String,
    val url: String
)

object AppInfo {
    val versionName: String = BuildConfig.VERSION_NAME
    val versionCode: Int = BuildConfig.VERSION_CODE
    
    val androidVersion: String = AndroidPlatform.versionLabel
    val deviceModel: String = AndroidPlatform.deviceLabel
    
    val dependencies = listOf(
        LicenseItem(
            name = "CyIME（Cayboard 的上游项目）",
            license = "GPL-3.0",
            url = "https://github.com/Cyletix/CyIME/blob/main/LICENSE"
        ),
        LicenseItem(
            name = "librime",
            license = "BSD-3-Clause",
            url = "https://github.com/rime/librime"
        ),
        LicenseItem(
            name = "AndroidX Core",
            license = "Apache-2.0",
            url = "https://developer.android.com/jetpack/androidx"
        ),
        LicenseItem(
            name = "Jetpack Compose",
            license = "Apache-2.0",
            url = "https://developer.android.com/jetpack/compose"
        ),
        LicenseItem(
            name = "Material Design Icons",
            license = "Apache-2.0",
            url = "https://fonts.google.com/icons"
        ),
        LicenseItem(
            name = "Kotlin",
            license = "Apache-2.0",
            url = "https://kotlinlang.org"
        ),
        LicenseItem(
            name = "Navigation Compose",
            license = "Apache-2.0",
            url = "https://developer.android.com/jetpack/compose/navigation"
        ),
        LicenseItem(
            name = "Lifecycle",
            license = "Apache-2.0",
            url = "https://developer.android.com/jetpack/androidx/releases/lifecycle"
        ),
        LicenseItem(
            name = "Boost",
            license = "BSL-1.0",
            url = "https://www.boost.org"
        ),
        LicenseItem(
            name = "OpenCC (Open Chinese Convert)",
            license = "Apache-2.0",
            url = "https://github.com/BYVoid/OpenCC"
        ),
        LicenseItem(
            name = "KaTeX",
            license = "MIT",
            url = "https://github.com/KaTeX/KaTeX"
        ),
        LicenseItem(
            name = "Xime（CyIME 的上游项目）",
            license = "GPL-3.0",
            url = "https://github.com/ximeiorg/Xime"
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutContent(
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToLogViewer: () -> Unit = {},
    onNavigateToHandwritingCapture: () -> Unit = {},
    onNavigateToStorageSpace: () -> Unit = {},
) {
    val context = LocalContext.current
    var verboseLoggingEnabled by remember {
        mutableStateOf(SettingsPreferences.isVerboseLoggingEnabled(context))
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("关于") },
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
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .settingsVerticalScroll()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App 信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cayboard 是基于 CyIME 和 Rime 开发的开源 Android 输入法，主打中文九键，支持全拼、双拼和英文输入。内置词库、手写、语音转文字、拍照识字和多语种翻译，均在设备上运行。应用代码采用 GNU GPLv3。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "版本 ${AppInfo.versionName} (${AppInfo.versionCode})",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                    }
                }
            
            // 链接项
                SettingsSection(title = "信息与工具") {
                        SettingsItem(
                            icon = Icons.TwoTone.PrivacyTip,
                            title = "隐私政策",
                            onClick = onNavigateToPrivacy
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsItem(
                            icon = Icons.TwoTone.Description,
                            title = "开源许可证",
                            onClick = onNavigateToLicenses
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsItem(
                            icon = Icons.TwoTone.BugReport,
                            title = "日志查看器",
                            onClick = onNavigateToLogViewer
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsItem(
                            icon = Icons.TwoTone.Storage,
                            title = "存储空间",
                            onClick = onNavigateToStorageSpace
                        )
                        if (BuildConfig.DEBUG) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            SettingsItem(
                                icon = Icons.TwoTone.BugReport,
                                title = "调试日志",
                                onClick = {},
                                trailing = {
                                    Switch(
                                        checked = verboseLoggingEnabled,
                                        onCheckedChange = { enabled ->
                                            verboseLoggingEnabled = enabled
                                            SettingsPreferences.setVerboseLoggingEnabled(context, enabled)
                                            FileLogger.setVerboseLoggingEnabled(enabled)
                                        }
                                    )
                                }
                            )
                        }
                }
            
            // 设备信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "设备信息",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(label = "系统版本", value = AppInfo.androidVersion)
                        InfoRow(label = "设备型号", value = AppInfo.deviceModel)
                    }
                }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyContent(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("隐私政策") },
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
                )
            )
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.settingsGutterScroll(listState)
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = keyboardPreviewContentPadding(),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Cayboard 隐私政策",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PrivacySection(
                            title = "离线处理",
                            content = "Cayboard 不具备联网权限，无需账号，不含广告或云同步，也不会上传诊断信息。文字输入、手写、语音识别、拍照识字和翻译均在设备上处理。词库和内置模型随安装包提供。"
                        )

                        PrivacySection(
                            title = "输入和剪贴板",
                            content = "Rime 在本机处理键盘输入及候选词。确认输入、粘贴或插入译文后，内容会交给当前输入框所在的应用。开启剪贴板历史时，符合记录条件的文本会保存在本机。可在剪贴板删除单条记录，或在「关于 → 存储空间」清理历史。"
                        )

                        PrivacySection(
                            title = "麦克风和相机",
                            content = "使用语音转文字时需要麦克风权限，录音在本机识别。使用拍照识字时需要相机权限，也可选择图片进行本地识别。"
                        )

                        PrivacySection(
                            title = "联系人与短信验证码",
                            content = "开启联系人联想并授权后，Cayboard 在本机查询联系人以生成候选。开启短信验证码功能并授权后，Cayboard 会处理新收到的短信，在本机提取验证码，并把验证码作为敏感内容放入系统剪贴板。"
                        )

                        PrivacySection(
                            title = "保存、分享和删除",
                            content = "设置、词库、剪贴板历史、模型文件及诊断日志保存在本机。主动导出备份或分享日志时，系统会把文件交给你选择的位置或应用。可在 Android 的应用权限页撤销授权，在存储空间页清理部分数据，或通过清除应用数据、卸载来删除应用内部数据。已导出的文件需另行删除。"
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "更新日期：2026 年 11 月 20 日",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacySection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesContent(
    onBack: () -> Unit
) {
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("开源许可证") },
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
                )
            )
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.settingsGutterScroll(listState)
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = keyboardPreviewContentPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(AppInfo.dependencies) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.license,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

