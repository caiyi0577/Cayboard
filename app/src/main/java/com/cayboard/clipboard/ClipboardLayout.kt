package com.cayboard.clipboard
internal const val CLIPBOARD_LIST_LAYOUT_KEY = "list_layout"
internal fun clipboardColumnCount(widthDp: Float): Int = (widthDp / 160f).toInt().coerceIn(1,5)
