package com.cayboard.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cayboard.settings.CloudBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal object BackupChoices {
    fun includePersonal(context: android.content.Context): Boolean = true
    fun setIncludePersonal(context: android.content.Context, include: Boolean) = Unit
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(CloudBackupPreferences.url(context)) }
    var username by remember { mutableStateOf(CloudBackupPreferences.username(context)) }
    var password by remember { mutableStateOf(CloudBackupPreferences.password(context)) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    fun run(action: suspend () -> Result<Long>) {
        CloudBackupPreferences.save(context, url, username, password)
        busy = true; message = null
        scope.launch {
            val result = withContext(Dispatchers.IO) { action() }
            busy = false
            message = result.fold({ "完成：${it / 1024 / 1024} MiB" }, { "失败：${it.message ?: "未知错误"}" })
        }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.surface,
        topBar = { TopAppBar(title = { Text("备份与恢复") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).settingsVerticalScroll(),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsSection(title = "坚果云 WebDAV") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.TwoTone.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                    Text("备份 Cayboard 的全部用户数据：设置、词库、模型、布局、剪贴板和应用文件。备份包直接上传坚果云，不在本机保留本地备份文件。")
                    OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("坚果云 WebDAV 目录") }, singleLine = true)
                    OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("坚果云注册邮箱") }, singleLine = true)
                    OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("第三方应用密码") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation())
                    Text("默认地址：${CloudBackupManager.DEFAULT_URL}。先在坚果云【第三方应用管理】中生成应用密码；备份目录建议使用英文，例如在地址后填写 Cayboard/。不要填写网页登录密码。",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { run { CloudBackupManager.backup(context, url, username, password) } }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth()) { if (busy) CircularProgressIndicator(strokeWidth = 2.dp); Text("备份全部数据到坚果云") }
                    Button(onClick = { run { CloudBackupManager.restore(context, url, username, password) } }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth()) { Text("从坚果云还原全部数据") }
                    message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

internal object CloudBackupPreferences {
    private const val PREFS = "cloud_backup"
    fun url(context: android.content.Context) = context.getSharedPreferences(PREFS, 0).getString("webdav_url", CloudBackupManager.DEFAULT_URL) ?: CloudBackupManager.DEFAULT_URL
    fun username(context: android.content.Context) = context.getSharedPreferences(PREFS, 0).let {
        CloudBackupManager.decryptCredential(context, it.getString("webdav_user", "") ?: "")
    }
    fun password(context: android.content.Context) = context.getSharedPreferences(PREFS, 0).let {
        CloudBackupManager.decryptCredential(context, it.getString("webdav_password", "") ?: "")
    }
    fun save(context: android.content.Context, url: String, user: String, password: String) = context.getSharedPreferences(PREFS, 0).edit()
        .putString("webdav_url", url)
        .putString("webdav_user", CloudBackupManager.encryptCredential(context, user))
        .putString("webdav_password", CloudBackupManager.encryptCredential(context, password)).apply()
}
