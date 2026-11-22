package com.cayboard.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cayboard.model.ModelCategory
import com.cayboard.model.ModelImporter
import com.cayboard.model.ModelDownloadState
import com.cayboard.model.ModelInfo
import com.cayboard.model.ModelManager
import com.cayboard.model.ModelVersion
import com.cayboard.settings.MarketVersionStore
import com.cayboard.speech.AsrModelManager
import com.cayboard.speech.SpeechModelCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ModelItemState(
    val model: ModelInfo,
    val isDownloaded: Boolean = false,
    val diskSize: Long = 0,
    val downloadState: ModelDownloadState = ModelDownloadState.Idle,
    /** 本地已下载的模型版本（无则未下载或版本未知） */
    val installedVersion: String? = null,
    val importedCurrent: Boolean = false,
    val archivedVersions: List<ModelVersionState> = emptyList(),
) {
    /** 是否已有更新：已下载且本地版本 != 索引默认版本。 */
    val hasUpdate: Boolean
        get() {
            if (!isDownloaded || installedVersion == null) return false
            val latest = model.resolvedVersion()?.version ?: return false
            return installedVersion != latest
        }
}

data class ModelVersionState(
    val version: String,
    val diskSize: Long,
)

data class ModelManagementUiState(
    val models: List<ModelItemState> = emptyList(),
    val isLoading: Boolean = true,
    val toastMessage: String? = null,
    /** 当前选中的模型 id → 版本字符串（空表示默认版本） */
    val selectedVersions: Map<String, String> = emptyMap(),
    /** 分类过滤：null 表示全部 */
    val selectedCategory: ModelCategory? = null,
    /** 正在从本机导入模型文件。 */
    val isImporting: Boolean = false,
) {
    /** 商店里按分类过滤后的模型列表；已全部退役的条目不再提供下载（已下载的仍在模型管理里可删）。 */
    val filteredModels: List<ModelItemState>
        get() = models.filter { it.model.availableVersions.isNotEmpty() &&
            (selectedCategory == null || it.model.category == selectedCategory) }
}

class ModelManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(ModelManagementUiState())
    val uiState: StateFlow<ModelManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            var completed = emptySet<String>()
            ModelManager.downloadStates.collectLatest { states ->
                _uiState.update { current ->
                    current.copy(models = current.models.map { item ->
                        val state = states[item.model.id]
                        if (state == null) item else item.copy(downloadState =
                            if (state is ModelDownloadState.Complete) ModelDownloadState.Idle else state)
                    })
                }
                val nowCompleted = states.filterValues { it is ModelDownloadState.Complete }.keys
                if ((nowCompleted - completed).isNotEmpty()) loadModels()
                completed = nowCompleted
            }
        }
        viewModelScope.launch {
            loadModels()
        }
    }

    private suspend fun loadModels() {
        // 模型管理展示完整清单，包括语音模型；下载页仍可按自己的规则过滤。
        val allModels = (ModelManager.getAllModels() + SpeechModelCatalog.models).distinctBy { it.id }
        val packaged = withContext(Dispatchers.IO) { com.cayboard.model.packagedPacks(context) }
        val versions = withContext(Dispatchers.IO) {
            MarketVersionStore.getAllModelVersions(context)
        }
        val items = allModels.map { model ->
            val downloaded = withContext(Dispatchers.IO) {
                if (model.id in packaged) true
                else if (model.category == ModelCategory.ASR) AsrModelManager(context).isXasrReady()
                else ModelManager.isModelDownloaded(context, model)
            }
            val size = if (downloaded) {
                withContext(Dispatchers.IO) {
                    if (model.id in packaged) model.resolvedVersion()?.files?.sumOf { it.sizeBytes } ?: 0L
                    else if (model.category == ModelCategory.ASR) {
                        modelStorageSize(context, model.id)
                    } else ModelManager.getModelSizeOnDisk(context, model.id)
                }
            } else 0L
            val installedVersion = if (downloaded) withContext(Dispatchers.IO) {
                if (model.id in packaged || model.category == ModelCategory.ASR) model.resolvedVersion()?.version
                else ModelManager.installedVersionOf(context, model)?.version
            } ?: versions[model.id] else null
            ModelItemState(
                model = model,
                isDownloaded = downloaded,
                diskSize = size,
                installedVersion = installedVersion,
                importedCurrent = installedVersion?.let { ModelManager.isImportedModelVersion(context, model.id, it) } == true,
                archivedVersions = withContext(Dispatchers.IO) {
                    ModelManager.getArchivedModelVersions(context, model.id)
                        .map { (version, size) -> ModelVersionState(version, size) }
                },
                downloadState = ModelManager.downloadStates.value[model.id]
                    ?.takeUnless { it is ModelDownloadState.Complete } ?: ModelDownloadState.Idle,
            )
        }
        _uiState.update { it.copy(models = items, isLoading = false) }
    }

    /** Recheck on-device models after a local import, deletion, or return to the market. */
    fun refresh() = refreshDownloadedState()

    fun refreshDownloadedState() {
        viewModelScope.launch { loadModels() }
    }

    fun selectVersion(modelId: String, version: String) {
        _uiState.update {
            it.copy(selectedVersions = it.selectedVersions + (modelId to version))
        }
    }

    fun selectCategory(category: ModelCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun downloadModel(modelId: String, version: ModelVersion? = null) {
        val index = _uiState.value.models.indexOfFirst { it.model.id == modelId }
        if (index < 0) return

        viewModelScope.launch {
            updateModelState(index) {
                it.copy(downloadState = ModelDownloadState.Downloading(0f, 0, -1))
            }

            var downloadError: String? = null
            val selectedVersion = version
                ?: _uiState.value.selectedVersions[modelId]?.let { versionStr ->
                    ModelManager.getModel(modelId)?.versions?.firstOrNull { it.version == versionStr }
                }
            ModelManager.downloadModel(context, _uiState.value.models[index].model, { state ->
                when (state) {
                    is ModelDownloadState.Downloading -> {
                        updateModelState(index) { it.copy(downloadState = state) }
                    }
                    is ModelDownloadState.Error -> {
                        downloadError = state.message
                        updateModelState(index) { it.copy(downloadState = state) }
                    }
                    else -> {}
                }
            }, selectedVersion)

            if (downloadError != null) {
                updateModelState(index) { it.copy(downloadState = ModelDownloadState.Idle) }
            } else {
                val downloaded = withContext(Dispatchers.IO) {
                    ModelManager.isModelDownloaded(context, modelId)
                }
                val size = if (downloaded) {
                    withContext(Dispatchers.IO) {
                        ModelManager.getModelSizeOnDisk(context, modelId)
                    }
                } else 0L
                // 版本由 ModelManager 与最终安装一起记录，UI 不在删除后补写旧下载版本。
                val actualVersion = if (downloaded) withContext(Dispatchers.IO) {
                    MarketVersionStore.getModelVersion(context, modelId)
                } else null
                updateModelState(index) {
                    it.copy(
                        isDownloaded = downloaded,
                        diskSize = size,
                        installedVersion = actualVersion,
                        downloadState = ModelDownloadState.Idle
                    )
                }
                if (downloaded) {
                    val message = if (ModelManager.getModel(modelId)?.category == com.cayboard.model.ModelCategory.CANDIDATE)
                        "已安装，重新启动输入法后生效" else "模型下载完成"
                    _uiState.update { s -> s.copy(toastMessage = message) }
                }
            }
        }
    }

    /** 从选中的文件、整合包或文件夹导入；按哈希自动认出属于哪个模型。 */
    fun importDocuments(uris: List<Uri>) = runImport { ModelImporter.importDocuments(context, uris) }

    fun importTree(treeUri: Uri) = runImport { ModelImporter.importTree(context, treeUri) }

    fun deleteArchivedVersion(modelId: String, version: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (ModelManager.deleteArchivedModelVersion(context, modelId, version)) loadModels()
        }
    }

    fun deleteImportedCurrent(modelId: String) {
        val model = _uiState.value.models.firstOrNull { it.model.id == modelId }?.model ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (ModelManager.deleteImportedCurrentModel(context, model)) loadModels()
        }
    }

    private fun runImport(block: suspend () -> ModelImporter.Report) {
        if (_uiState.value.isImporting) return
        _uiState.update { it.copy(isImporting = true) }
        viewModelScope.launch {
            val message = try { block().message() } catch (e: Exception) { "导入失败：${e.message}" }
            loadModels()
            _uiState.update { it.copy(isImporting = false, toastMessage = message) }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun updateModelState(index: Int, transform: (ModelItemState) -> ModelItemState) {
        _uiState.update { state ->
            val updated = state.models.toMutableList()
            updated[index] = transform(updated[index])
            state.copy(models = updated)
        }
    }

    private fun modelStorageSize(context: android.content.Context, id: String): Long {
        fun size(file: java.io.File): Long = if (file.isFile) file.length()
        else file.listFiles().orEmpty().sumOf(::size)
        return size(com.cayboard.model.ModelStorage.getModelDir(context, id))
    }
}
