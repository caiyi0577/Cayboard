package com.cayboard.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asContextElement

/** Ownership travels with the coroutine, including dispatcher switches. */
internal data class InputCommandOwner(val admission: Long, val editor: Long, val validate: () -> Unit) {
    fun context() = current.asContextElement(this) +
        com.cayboard.rime.RimeCommandContext.validity.asContextElement(validate)

    fun <T> runInline(block: () -> T): T {
        val previous = current.get()
        val previousValidator = com.cayboard.rime.RimeCommandContext.validity.get()
        current.set(this)
        com.cayboard.rime.RimeCommandContext.validity.set(validate)
        try { validate(); return block() } finally {
            current.set(previous)
            com.cayboard.rime.RimeCommandContext.validity.set(previousValidator)
        }
    }

    fun requireCurrent(readiness: InputReadiness, editor: Long) {
        if (!readiness.accepts(admission) || this.editor != editor) {
            throw CancellationException("Input command belongs to an expired editor session")
        }
    }

    companion object {
        val current = ThreadLocal<InputCommandOwner?>()
        fun requireOwner(): InputCommandOwner = checkNotNull(current.get()) {
            "Queued input must carry its original command owner"
        }
    }
}
