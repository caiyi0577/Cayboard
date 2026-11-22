package com.cayboard.speech

import android.content.Context
import com.cayboard.model.ModelCategory
import com.cayboard.model.ModelManager
import com.cayboard.model.ModelStorage
import com.cayboard.settings.InputLanguage
import java.io.File

/**
 * ASR 模型管理与选择。
 *
 * 使用官方 sherpa-onnx；保留原市场 Zipformer，补充 ASRInput 模型和双模型选择。
 * 模型清单与描述来自「扩展商店」远程索引（[ModelManager]，category=asr），
 * 新语音模型也有本地固定索引；索引离线时保留 Zipformer 兼容入口。
 */
class AsrModelManager(private val context: Context) {

    companion object {
        /** 内置默认 ASR 模型（远程索引加载前/失败时的兜底）。 */
        val DEFAULT_MODEL = AsrModelInfo(
            id = "zipformer-zh-int8",
            name = "Zipformer · 中文",
            description = "Zipformer 架构，适合实时语音识别，int8 量化",
            language = "zh",
            size = "132.63MB",
            modelType = "transducer",
            files = listOf("encoder.int8.onnx", "decoder.onnx", "joiner.int8.onnx", "tokens.txt"),
            encoderFile = "encoder.int8.onnx",
            decoderFile = "decoder.onnx",
            joinerFile = "joiner.int8.onnx"
        )

        /** 把索引里的 ModelInfo 转换为 ASR 专用模型信息。 */
        private fun toAsrModelInfo(info: com.cayboard.model.ModelInfo): AsrModelInfo {
            val version = info.resolvedVersion()
            val fileNames = info.files.map { it.name }
            val japanese = SpeechModelCatalog.isJapaneseZipformer(info.id)
            val japaneseSuffix = if (info.id == SpeechModelCatalog.ZIPFORMER_JA_INT8) ".int8.onnx" else ".onnx"
            return AsrModelInfo(
                id = info.id,
                name = SpeechModelSelection.displayName(info.id) ?: info.name,
                description = info.description,
                language = when (info.id) { SpeechModelCatalog.ZIPFORMER_JA, SpeechModelCatalog.ZIPFORMER_JA_INT8 -> "ja"; SpeechModelCatalog.SENSEVOICE -> "auto"; SpeechModelCatalog.PARAFORMER -> "zh-en"; else -> "zh" },
                size = version?.size ?: info.size,
                modelType = when (info.id) { SpeechModelCatalog.ZIPFORMER_JA, SpeechModelCatalog.ZIPFORMER_JA_INT8 -> "offline-transducer"; SpeechModelCatalog.PARAFORMER -> "paraformer"; SpeechModelCatalog.SENSEVOICE -> "sensevoice"; else -> "transducer" },
                files = fileNames,
                encoderFile = if (japanese) "encoder-epoch-99-avg-1$japaneseSuffix" else "encoder.int8.onnx",
                decoderFile = if (japanese) "decoder-epoch-99-avg-1$japaneseSuffix" else if (info.id == SpeechModelCatalog.PARAFORMER) "decoder.int8.onnx" else "decoder.onnx",
                joinerFile = if (japanese) "joiner-epoch-99-avg-1$japaneseSuffix" else "joiner.int8.onnx"
            )
        }
    }

    data class AsrModelInfo(
        val id: String,
        val name: String,
        val description: String = "",
        val language: String,
        val size: String,
        val modelType: String = "transducer",
        val files: List<String>,
        val encoderFile: String = "",
        val decoderFile: String = "",
        val joinerFile: String = ""
    )

    /** ASR 分类的模型清单（索引优先，索引未加载时用内置默认）。 */
    fun getAsrModels(): List<AsrModelInfo> {
        return SpeechModelCatalog.models.filter { it.id == SpeechModelCatalog.XASR }.map(::toAsrModelInfo)
    }

    fun isModelReady(): Boolean = try { selection().ready } catch (_: Exception) { false }

    data class Selection(
        val mode: String,
        val first: AsrModelInfo?, val firstDir: File?, val secondDir: File?,
        val recognitionLanguage: String = "auto",
        /** 整句定稿用的 X-ASR 目录；为 null 时整句定稿用 SenseVoice。 */
        val finalizerDir: File? = null,
    ) {
        val ready: Boolean get() = (finalizerDir == null || listOf("encoder-epoch-99-avg-1.int8.onnx", "decoder-epoch-99-avg-1.onnx", "joiner-epoch-99-avg-1.int8.onnx", "tokens.txt").all { File(finalizerDir, it).let { f -> f.isFile && f.length() > 0 } }) && (first == null || firstDir != null && first.files.isNotEmpty() && first.files.all { File(firstDir, it).let { f -> f.isFile && f.length() > 0 } }) &&
            (secondDir == null || listOf("model.int8.onnx", "tokens.txt").all { File(secondDir, it).let { f -> f.isFile && f.length() > 0 } })
        val key: String get() = listOfNotNull(firstDir, secondDir, finalizerDir).joinToString(prefix = "$mode:$recognitionLanguage:") { dir ->
            dir.absolutePath + ":" + dir.listFiles().orEmpty().filter { it.isFile }.sortedBy { it.name }
                .joinToString { "${it.name}:${it.length()}:${it.lastModified()}" }
        }
    }

    fun selection(mode: String = getSelectedModelId(), language: InputLanguage? = null): Selection {
        require(language == null || language == InputLanguage.CHINESE || language == InputLanguage.ENGLISH) {
            "语音识别支持中文和英文"
        }
        return Selection(SpeechModelCatalog.XASR, null, null, null,
            if (language == InputLanguage.ENGLISH) "en" else "zh",
            ModelStorage.getModelDir(context, SpeechModelCatalog.XASR))
    }

    /** X-ASR for 整句定稿, when fully downloaded; when it applies is [SpeechModelSelection.xasrFinalizes]. */
    fun wholeSentenceFinalizer(selection: Selection): Selection = selection

    /** Every X-ASR file is on disk; the settings page shows the same check the :asr service uses. */
    fun isXasrReady(): Boolean {
        val info = getAsrModels().firstOrNull { it.id == SpeechModelCatalog.XASR } ?: return false
        val dir = ModelStorage.getModelDir(context, SpeechModelCatalog.XASR)
        return info.files.isNotEmpty() && info.files.all { File(dir, it).let { f -> f.isFile && f.length() > 0 } }
    }

    /** Called on the speech worker: model file checks/migration never enter the key path. */
    fun selectionForLanguage(language: InputLanguage): Selection {
        FixedSpeechModel.prepare(context)
        val mode = getSelectedModelId(language)
        return selection(mode, language).also { check(it.ready) { "内置语音模型准备失败" } }
    }

    private val preferences get() = context.getSharedPreferences("asr_model", Context.MODE_PRIVATE)

    /** Persist the complete selection as one value, also passed explicitly to the :asr process. */
    fun getSelectedModelId(): String = SpeechModelCatalog.XASR

    /** Language choices are independent. Legacy global choices remain a read-only migration fallback. */
    fun getSelectedModelId(language: InputLanguage): String {
        require(language == InputLanguage.CHINESE || language == InputLanguage.ENGLISH) { "语音识别支持中文和英文" }
        return SpeechModelCatalog.XASR
    }

    /**
     * Unchosen Chinese/English default: Paraformer alone misrecognizes about one character in
     * five (2026-10-07 FLEURS, 18% CER); once SenseVoice is downloaded, correct each pause with it.
     * The 完整版 without Paraformer starts on SenseVoice alone; once Paraformer is downloaded it gets the
     * same streaming preview + SenseVoice. Any explicit per-language or legacy choice, including correction off, is kept.
     */
    private fun withDefaultCorrection(mode: String, language: InputLanguage): String =
        if ((mode == SpeechModelCatalog.PARAFORMER ||
                mode == SpeechModelCatalog.SENSEVOICE && SpeechLanguages.supports(SpeechModelCatalog.TWO_PASS, language)) &&
            !preferences.contains("selected_model") &&
            runCatching { selection(SpeechModelCatalog.TWO_PASS).ready }.getOrDefault(false)) SpeechModelCatalog.TWO_PASS
        else mode

    fun setModel(modelId: String, language: InputLanguage) {
        require(SpeechLanguages.supports(modelId, language)) { "所选模型不支持${language.displayName}语音" }
        preferences.edit().putString("selected_model_${language.id}", modelId).apply()
    }

    fun setFirstPassModel(modelId: String, language: InputLanguage) =
        setModel(SpeechModelSelection.selectPrimary(getSelectedModelId(language), modelId), language)

    fun setRefinementEnabled(enabled: Boolean, language: InputLanguage) =
        setModel(SpeechModelSelection.withCorrection(getSelectedModelId(language), enabled), language)

    fun getFirstPassModelId(): String = SpeechModelSelection.primary(getSelectedModelId())

    fun isRefinementEnabled(): Boolean = SpeechModelSelection.hasCorrection(getSelectedModelId())

    fun setFirstPassModel(modelId: String) {
        setModel(SpeechModelSelection.selectPrimary(getSelectedModelId(), modelId))
    }

    fun setRefinementEnabled(enabled: Boolean) {
        setModel(SpeechModelSelection.withCorrection(getSelectedModelId(), enabled))
    }

    fun setModel(modelId: String) {
        preferences.edit().putString("selected_model", modelId).apply()
    }
}
