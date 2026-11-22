package com.cayboard.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cayboard.rime.BundledT9Reranker
import com.cayboard.translate.BundledTranslationModel
import com.cayboard.viewmodel.ModelItemState
import com.cayboard.viewmodel.ModelManagementViewModel

/** 本地模型管理：列出已安装模型并提供本机导入。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelLocalContent(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: ModelManagementViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模型管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        val localModels = uiState.models
        val builtinT9Reranker = remember(context) { BundledT9Reranker.isBundled(context) }
        // 带模型的安装包内置拍照识别模型；轻量版下载后和其他模型一样列在下面。
        val builtinTranslation = remember(context) { BundledTranslationModel.refresh(context) }
        // 三段都用设置页统一的分组样式：标题 + 一个圆角面板，行样式一致，说明文字只放在所属分组里。
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.settingsGutterScroll(listState)
                .fillMaxSize()
                .padding(padding)
                .testTag("model-local-list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = SETTINGS_BOTTOM_SPACE),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "models") {
                val modelCount = localModels.size + listOf(builtinTranslation, builtinT9Reranker).count { it }
                SettingsSection(title = "离线模型($modelCount)") {
                    if (builtinT9Reranker) {
                        ModelRow(
                            name = "九键候选重排模型",
                            tag = "已安装",
                            detail = "版本 rerank-v2 · 容量 8.2 MB",
                            modifier = Modifier.testTag("builtin-t9-reranker"),
                        )
                    }
                    if (builtinT9Reranker && localModels.isNotEmpty()) ModelRowDivider()
                    localModels.forEachIndexed { index, item ->
                        if (index > 0 || builtinT9Reranker) ModelRowDivider()
                        ModelLocalRow(item = item, onDeleteCurrent = { viewModel.deleteImportedCurrent(item.model.id) })
                    }
                    if (localModels.isEmpty() && uiState.isLoading) {
                        ModelRowDivider()
                        ModelSectionNote("正在读取本机模型…")
                    }
                    run {
                        if (localModels.isNotEmpty() || builtinT9Reranker) ModelRowDivider()
                        ModelRow("Hy-MT2-1.8B", if (builtinTranslation) "已安装" else "未释放",
                            "版本 ${BundledTranslationModel.VERSION} · 容量 ${BundledTranslationModel.SIZE_LABEL}",
                            modifier = Modifier.testTag("builtin-translation-model"))
                    }
                    ModelRowDivider()
                    ModelSectionNote("每行显示模型名称、版本和容量；未安装模型仍会列出，容量取清单中的预估值。")
                }
            }
            item(key = "model-import") {
                ModelImportSection(
                    isImporting = uiState.isImporting,
                    onImportFiles = viewModel::importDocuments,
                    onImportFolder = viewModel::importTree,
                )
            }
        }
    }
}

/** 从本机导入：把自己下好的文件导进来。 */
@Composable
private fun ModelImportSection(
    isImporting: Boolean,
    onImportFiles: (List<Uri>) -> Unit,
    onImportFolder: (Uri) -> Unit,
) {
    val filesContract = remember {
        object : ActivityResultContracts.OpenMultipleDocuments() {
            override fun createIntent(context: Context, input: Array<String>): Intent =
                super.createIntent(context, input).putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        }
    }
    val folderContract = remember {
        object : ActivityResultContracts.OpenDocumentTree() {
            override fun createIntent(context: Context, input: Uri?): Intent =
                super.createIntent(context, input).putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        }
    }
    val filesLauncher = rememberLauncherForActivityResult(filesContract) { uris ->
        if (uris.isNotEmpty()) onImportFiles(uris)
    }
    val folderLauncher = rememberLauncherForActivityResult(folderContract) { uri ->
        uri?.let(onImportFolder)
    }
    SettingsSection(title = "从本机导入") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("可选模型文件、官方压缩包、整合包(.zip)，或一个存放模型的文件夹；按文件内容自动认出是哪个模型，认不出的不会安装。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { filesLauncher.launch(arrayOf("*/*")) }, enabled = !isImporting,
                    modifier = Modifier.testTag("model-import-files")) { Text("选择文件") }
                OutlinedButton(onClick = { folderLauncher.launch(null) }, enabled = !isImporting,
                    modifier = Modifier.testTag("model-import-folder")) { Text("选择文件夹") }
                if (isImporting) Text("正在导入…", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ModelLocalRow(
    item: ModelItemState,
    onDeleteCurrent: () -> Unit,
) {
    val version = item.installedVersion ?: item.model.resolvedVersion()?.version ?: "未知"
    val size = if (item.diskSize > 0) formatSize(item.diskSize)
        else item.model.resolvedVersion()?.size?.takeIf { it.isNotBlank() } ?: formatSize(item.model.resolvedVersion()?.files?.sumOf { it.sizeBytes } ?: 0L)
    val detail = "版本 $version · 容量 $size"
    ModelRow(
        name = item.model.name,
        tag = if (item.isDownloaded) "已安装" else "未安装",
        detail = detail,
        trailing = if (item.importedCurrent) {
            { IconButton(onClick = onDeleteCurrent) {
                Icon(Icons.Default.Delete, contentDescription = "删除导入版本", tint = MaterialTheme.colorScheme.error)
            } }
        } else null,
    )
    item.archivedVersions.forEach { archived ->
        ModelRowDivider()
        ModelRow(
            name = "${item.model.name}（旧版本）",
            tag = "本地旧版",
            detail = "版本 ${archived.version} · 容量 ${formatSize(archived.diskSize)}",
        )
    }
}

@Composable
private fun ModelRow(
    name: String,
    tag: String,
    detail: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                TagText(tag, MaterialTheme.colorScheme.primary)
            }
            if (detail.isNotEmpty()) {
                Text(detail, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { trailing?.invoke() }
    }
}

@Composable
private fun ModelSectionNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
}

@Composable
private fun ModelRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}


@Composable
private fun TagText(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private fun formatSize(bytes: Long): String = if (bytes <= 0) "未知大小" else "%.1f MB".format(bytes / 1024f / 1024f)


