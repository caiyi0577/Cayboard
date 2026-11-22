package com.cayboard.model

enum class ModelCategory {
    CANDIDATE,
    ASR,
    HANDWRITING,
    OTHER
}

data class ModelFile(
    val name: String,
    val sha256: String = "",
    val sizeBytes: Long = 0,
)

/** 单个模型版本：文件清单 + 归档 + 元信息。 */
data class ModelVersion(
    val version: String = "",
    val date: String = "",
    val changelog: String = "",
    val files: List<ModelFile> = emptyList(),
    val archive: Boolean = false,
    val size: String = "",
    val sha256: String = "",
    /** 已退役：只用来认出、删除设备上的旧文件，商店不再提供下载。 */
    val retired: Boolean = false,
)

data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val category: ModelCategory,
    val versions: List<ModelVersion> = emptyList(),
    val size: String = ""
) {
    /** 默认取第一个版本（索引中第一个为最新）。 */
    fun resolvedVersion(): ModelVersion? = versions.firstOrNull()

    /** 兼容访问器：委托到默认版本，现有 ModelManager/ModelDownloader 逻辑不感知版本。 */
    val files: List<ModelFile> get() = resolvedVersion()?.files ?: emptyList()

    /** 商店里可下载的版本；全部退役的条目不在商店出现，只在模型管理里供删除。 */
    val availableVersions: List<ModelVersion> get() = versions.filterNot { it.retired }
}

sealed class ModelDownloadState {
    data object Idle : ModelDownloadState()
    data class Downloading(
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
    data object Complete : ModelDownloadState()
}
