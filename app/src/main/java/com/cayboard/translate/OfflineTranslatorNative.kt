package com.cayboard.translate

/** CPU-only Hy-MT2 runtime with the pinned STQ1_0 ARM dot-product kernel. */
internal object OfflineTranslatorNative {
    @JvmStatic external fun nativeCreate(model: String, threads: Int): Long
    @JvmStatic external fun nativeTranslate(handle: Long, text: String, source: String, target: String): String
    @JvmStatic external fun nativeCancel()
    @JvmStatic external fun nativeDestroy(handle: Long)
    private var loaded: Boolean? = null
    @Synchronized fun load(): Boolean = loaded ?: runCatching { System.loadLibrary("cyhymt") }.isSuccess.also { loaded = it }
}
