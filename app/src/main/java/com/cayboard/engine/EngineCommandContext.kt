package com.cayboard.engine

/** Host supplies command validity; the engine checks it after acquiring its lock. */
internal object EngineCommandContext {
    val validity = ThreadLocal<(() -> Unit)?>()
    fun check() { validity.get()?.invoke() }
}
