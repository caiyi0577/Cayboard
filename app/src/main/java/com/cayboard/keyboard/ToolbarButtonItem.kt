package com.cayboard.keyboard

/** Keyboard actions implemented by the application. */
sealed class ToolbarButtonItem {
    abstract val id: String
    abstract val label: String

    data class Builtin(val button: ToolbarButton) : ToolbarButtonItem() {
        override val id: String get() = button.id
        override val label: String get() = button.label
    }
}

fun resolveToolbarButtonItem(id: String): ToolbarButtonItem? =
    ToolbarButton.fromId(id)?.let { ToolbarButtonItem.Builtin(it) }
