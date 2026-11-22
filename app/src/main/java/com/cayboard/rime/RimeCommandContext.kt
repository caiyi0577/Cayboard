package com.cayboard.rime

/** Host supplies command validity; the engine checks it after acquiring its lock. */
internal object RimeCommandContext {
    val validity = ThreadLocal<(() -> Unit)?>()
    fun check() { validity.get()?.invoke() }
}
