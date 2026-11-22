package com.cayboard.settings

import android.content.Context
import com.cayboard.clipboard.ClipboardManager
import com.cayboard.model.ModelManager
import com.cayboard.clipboard.db.ClipboardDatabase
import com.cayboard.ocr.OcrModelFiles
import com.cayboard.util.FileLogger
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes

/**
 * 存储空间分类统计与清除（设置 → 关于 → 存储空间）。
 *
 * 类目语义（清除安全性从高到低）：
 * - cache        cacheDir 整体（图片/临时下载缓存），清除后按需重建
 * - logs         filesDir/logs（保留 FileLogger 当日活跃文件，其句柄仍被持有）
 * - schema_cache rime/build 编译产物 + rime/logs；清除后复位部署标记，
 *                下次启动 ensureDeployment 自动全量重建，词库/自定义配置不受影响
 * - clipboard    Room 数据库文件（clipboard.db*）；清除走 ClipboardManager 的
 *                剪贴板历史清空
 * - models       filesDir/models 中的本机模型文件
 *
 * 各类目按顺序统计，同一文件的硬链接（如九键词组模型同时在 models/ 和 rime/）只计入先出现的类目，
 * 总数与系统设置里的应用数据对得上。
 */
object StorageStats {

    const val ID_CACHE = "cache"
    const val ID_LOGS = "logs"
    const val ID_SCHEMA_CACHE = "schema_cache"
    const val ID_CLIPBOARD = "clipboard"
    const val ID_MODELS = "models"
    const val ID_SCHEMAS = "schemas"
    const val ID_OCR_MODELS = "ocr_models"
    const val ID_EXTERNAL = "external"
    const val ID_OTHER = "other"

    /** 统计类目（只读信息；清除能力见 [clearCategory]）。 */
    data class Category(
        val id: String,
        val title: String,
        val description: String,
        val sizeBytes: Long,
        /** 提供就地"清理"按钮 */
        val clearable: Boolean,
        /** 点击行跳转对应管理页 */
        val navigable: Boolean = false,
    )

    // ── 路径 ──

    fun modelsDir(context: Context): File = File(context.filesDir, "models")
    fun rimeDir(context: Context): File = File(context.filesDir, "rime")
    fun rimeBuildDir(context: Context): File = File(context.filesDir, "rime/build")
    fun rimeLogsDir(context: Context): File = File(context.filesDir, "rime/logs")
    fun logsDir(context: Context): File = File(context.filesDir, "logs")
    /** 旧版本的语音模型目录；新版首次使用时迁到 models/，没迁走的残留也算模型。 */
    fun legacyAsrModelsDir(context: Context): File = File(context.filesDir, "asr_models")

    // ── 统计 ──

    /** 递归统计目录大小；目录不存在返回 0。 */
    fun directorySize(dir: File?): Long = SizeCounter().count(dir)

    /** 跨多次统计记住已经算过的文件和目录：硬链接和重叠目录只计一次，不跟随符号链接。 */
    class SizeCounter {
        private val seen = HashSet<Any>()

        fun count(file: File?): Long {
            if (file == null) return 0
            val attributes = runCatching {
                Files.readAttributes(file.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
            }.getOrNull() ?: return 0
            if (attributes.isSymbolicLink) return 0
            // Unix 上 fileKey 是设备号 + inode，硬链接相同；拿不到时退回路径。
            if (!seen.add(attributes.fileKey() ?: file.absoluteFile.normalize().path)) return 0
            if (attributes.isDirectory) return file.listFiles()?.sumOf { count(it) } ?: 0L
            return attributes.size()
        }
    }

    /** 汇总各类目占用（IO 耗时，须在后台线程调用）。 */
    fun collectCategories(context: Context): List<Category> {
        val counter = SizeCounter()
        // 顺序决定硬链接归属：模型在前，rime/ 里指向同一份模型的链接不再计入方案。
        val modelsSize = counter.count(modelsDir(context)) + counter.count(legacyAsrModelsDir(context))
        val ocrSize = counter.count(OcrModelFiles.extractedStorageDir(context))
        val buildSize = counter.count(rimeBuildDir(context)) + counter.count(rimeLogsDir(context))
        val schemasSize = counter.count(rimeDir(context)) + counter.count(SchemaManager.getMarketDir(context))
        val clipboardSize = ClipboardDatabase.storageFiles(context).sumOf { counter.count(it) }
        val logsSize = counter.count(logsDir(context))
        val cacheSize = counter.count(context.cacheDir) + counter.count(context.externalCacheDir)
        val externalSize = counter.count(context.getExternalFilesDir(null))
        val otherSize = counter.count(context.dataDir)

        return listOf(
            Category(
                id = ID_MODELS,
                title = "离线模型",
                description = "保存在数据目录里的模型文件，内置模型可从安装包恢复",
                sizeBytes = modelsSize,
                clearable = true,
                // 点进模型管理逐个删（11-04「更多对内存的控制」）；「清理」仍是一次全删。
                navigable = true,
            ),
            Category(
                id = ID_SCHEMA_CACHE,
                title = "方案编译缓存",
                description = "输入方案的编译产物与日志，清除后下次启动自动重建，不影响词库与自定义配置",
                sizeBytes = buildSize,
                clearable = true,
            ),
            Category(
                id = ID_SCHEMAS,
                title = "方案与词库",
                description = "输入方案、词库、自定义配置和本地方案归档",
                sizeBytes = schemasSize,
                clearable = false,
            ),

            Category(
                id = ID_CLIPBOARD,
                title = "剪贴板历史",
                description = "本地文字剪贴板记录",
                sizeBytes = clipboardSize,
                clearable = true,
            ),
            Category(
                id = ID_LOGS,
                title = "日志",
                description = "运行日志文件(保留当日日志)",
                sizeBytes = logsSize,
                clearable = true,
            ),
            Category(
                id = ID_CACHE,
                title = "缓存",
                description = "图片与临时文件缓存，可安全清理",
                sizeBytes = cacheSize,
                clearable = true,
            ),
            Category(
                id = ID_OCR_MODELS,
                title = "文字识别模型",
                description = "从安装包解出的本地文字识别模型文件",
                sizeBytes = ocrSize,
                clearable = false,
            ),
            Category(
                id = ID_EXTERNAL,
                title = "外部目录数据",
                description = "应用保存在 Android/data 下的文件",
                sizeBytes = externalSize,
                clearable = false,
            ),
            Category(
                id = ID_OTHER,
                title = "其他数据",
                description = "设置、解出的离线翻译模型及其他应用数据",
                sizeBytes = otherSize,
                clearable = false,
            ),
        ).filterNot { it.sizeBytes == 0L && it.id in setOf(ID_OCR_MODELS, ID_EXTERNAL) }
    }

    fun totalSize(categories: List<Category>): Long = categories.sumOf { it.sizeBytes }

    // ── 清除 ──

    /**
     * 清除指定类目（IO 耗时，须在后台线程调用）。
     * @return 是否有内容被清理（空目录清理也返回 true，UI 不区分）
     */
    suspend fun clearCategory(context: Context, id: String): Boolean {
        return when (id) {
            ID_MODELS -> clearModels(context)
            ID_SCHEMA_CACHE -> clearSchemaCache(context)
            ID_CLIPBOARD -> {
                ClipboardManager.getInstance(context).clearClipboard()
                true
            }
            ID_LOGS -> clearLogs(context)
            ID_CACHE -> deleteContents(context.cacheDir)
            else -> false
        }
    }

    private fun clearModels(context: Context): Boolean {
        var ok = true
        for (model in ModelManager.getAllModels()) {
            if (!ModelManager.deleteModel(context, model)) ok = false
        }
        // 未知来源的残余模型目录（如旧版本残留）一并移除
        modelsDir(context).listFiles()?.forEach { dir ->
            if (!dir.deleteRecursively()) ok = false
        }
        if (!legacyAsrModelsDir(context).deleteRecursively()) ok = false
        return ok
    }

    /**
     * 清除 rime 编译产物与日志。必须同时复位部署标记：ensureDeployment 在
     * 部署 hash 命中时会直接跳过编译，build 缺失而 hash 残留会导致引擎无产物可用。
     */
    private fun clearSchemaCache(context: Context): Boolean {
        val ok = deleteContents(rimeBuildDir(context)) && deleteContents(rimeLogsDir(context))
        SettingsPreferences.setDeploymentHash(context, "")
        SettingsPreferences.setDeploymentDone(context, false)
        return ok
    }

    private fun clearLogs(context: Context): Boolean {
        // 当日活跃文件（各进程各一个）被 FileLogger 持有句柄，删除后空间不会释放且新日志丢失，保留
        val dir = logsDir(context)
        var ok = true
        dir.listFiles()?.forEach { f ->
            if (!FileLogger.isActiveLogName(f.name) && !f.delete()) ok = false
        }
        FileLogger.i("StorageStats", "Logs cleared (kept today's active files)")
        return ok
    }

    /** 清空目录内容（保留目录本身），返回是否全部成功。 */
    private fun deleteContents(dir: File): Boolean {
        if (!dir.exists()) return true
        var ok = true
        dir.listFiles()?.forEach { f ->
            if (!f.deleteRecursively()) ok = false
        }
        return ok
    }
}
