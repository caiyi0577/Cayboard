package com.cayboard.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.*
import java.io.File

/** Fixed bilingual offline recognizer. No SenseVoice, streaming model, or online provider. */
internal class SherpaSpeechEngine(context: Context, val selection: AsrModelManager.Selection) : LocalSpeechEngine {
    private var recognizer: OfflineRecognizer? = null
    private var vad: Vad? = null
    override val hasRefinement = false
    override val isStreaming = false
    override val lockedChinese get() = selection.recognitionLanguage == "zh"
    init {
        try {
            val dir = checkNotNull(selection.finalizerDir)
            fun path(name: String) = File(dir, name).also { require(it.isFile && it.length() > 0) }.absolutePath
            recognizer = OfflineRecognizer(config = OfflineRecognizerConfig(modelConfig = OfflineModelConfig(
                transducer = OfflineTransducerModelConfig(path("encoder-epoch-99-avg-1.int8.onnx"),
                    path("decoder-epoch-99-avg-1.onnx"), path("joiner-epoch-99-avg-1.int8.onnx")),
                tokens = path("tokens.txt"), numThreads = 2, provider = "cpu"), decodingMethod = "greedy_search"))
            vad = Vad(context.assets, VadModelConfig(sileroVadModelConfig = SileroVadModelConfig(
                model = "speech/silero_vad.onnx", threshold = .6f, minSilenceDuration = .064f,
                minSpeechDuration = .15f, windowSize = 512, maxSpeechDuration = 30f)))
        } catch (error: Throwable) { close(); throw error }
    }
    override fun detector(): SpeechDetector = object : SpeechDetector {
        private val level = SpeechDetectionLevel()
        override fun speech(frame: FloatArray): Boolean {
            val v = checkNotNull(vad)
            v.acceptWaveform(level.process(frame))
            val active = v.isSpeechDetected()
            while (!v.empty()) v.pop()
            return active
        }
        override fun reset() { vad?.reset(); level.reset() }
    }
    override fun accept(samples: FloatArray): String = ""
    override fun finishStream(): String = ""
    private fun decode(samples: FloatArray, marks: Boolean): String {
        val model = checkNotNull(recognizer)
        val stream = model.createStream()
        try {
            // This export requires >=1440 samples and <40.2s; the production segmenter caps regions at 30s.
            require(samples.size <= 30 * 16000)
            stream.acceptWaveform(if (samples.size < 4000) samples.copyOf(4000) else samples, 16000)
            model.decode(stream)
            val result = model.getResult(stream)
            return cleanSenseVoiceText(result.text, result.lang, keepMarks = marks)
        } finally { stream.release() }
    }
    override fun refine(samples: FloatArray) = decode(samples, false)
    override fun refinePunctuated(samples: FloatArray) = decode(samples, true)
    override fun preview(samples: FloatArray) = decode(samples, true)
    override fun reset() { vad?.reset() }
    override fun close() { recognizer?.release(); recognizer = null; vad?.release(); vad = null }
}
