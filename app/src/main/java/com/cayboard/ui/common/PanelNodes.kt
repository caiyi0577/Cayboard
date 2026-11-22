package com.cayboard.ui.common

/** Local language controls shared by the translation bar and its settings. */
enum class UiNodeType { TEXT, SELECT, BUTTON }
data class UiNode(
    val type: UiNodeType,
    val key: String? = null,
    val label: String? = null,
    val value: String? = null,
    val options: List<String> = emptyList(),
    val style: String? = null,
)
