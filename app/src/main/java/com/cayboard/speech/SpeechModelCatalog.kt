package com.cayboard.speech

import com.cayboard.model.*

/** Pinned official exports, independently available even if the original market is offline. */
object SpeechModelCatalog {
    const val ZIPFORMER = "zipformer-zh-int8"
    const val ZIPFORMER_JA = "zipformer-ja-reazonspeech-fp32"
    const val ZIPFORMER_JA_INT8 = "zipformer-ja-reazonspeech-int8"
    fun isJapaneseZipformer(id: String) = id == ZIPFORMER_JA || id == ZIPFORMER_JA_INT8
    const val ZIPFORMER_TWO_PASS = "zipformer-sensevoice"
    const val PARAFORMER = "paraformer-zh-en-int8"
    const val SENSEVOICE = "sensevoice-multilingual-int8"
    const val TWO_PASS = "paraformer-sensevoice"
    /** 整句定稿的可选模型：X-ASR 中英 Zipformer（离线、自带标点）；何时代替 SenseVoice 见 [SpeechModelSelection.xasrFinalizes]。 */
    const val XASR = "xasr-zh-en-punct-int8"
    val models = listOf(
        ModelInfo(XASR, "X-ASR · 中英文语音识别", "可选。中文开着「整句定稿」时（只用 SenseVoice，或 Paraformer / Zipformer 加 SenseVoice 二次校正），下载后改用它定稿：更准、更快；只用于中文。", ModelCategory.ASR,
            versions = listOf(ModelVersion(version = "2026-06-03", size = "136.4MB", archive = true, sha256 = "5d02c36d7b44e886b7c8f0d8e051f8713acab96c264bb6ef9e718be39a6a2224",
                files = listOf(
                    ModelFile("encoder-epoch-99-avg-1.int8.onnx", "", 0L),
                    ModelFile("decoder-epoch-99-avg-1.onnx", "", 0L),
                    ModelFile("joiner-epoch-99-avg-1.int8.onnx", "", 0L),
                    ModelFile("tokens.txt", "", 0L),
                ))))
    )
}
