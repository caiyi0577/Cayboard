package com.cayboard.ui

import com.cayboard.keyboard.ToolbarAction
import com.cayboard.keyboard.ToolbarButton
import com.cayboard.keyboard.ToolbarButtonItem

/** Menu and toolbar share one action list; tests build it from the same built-in entries. */
internal fun quickActions(buttons: List<ToolbarButton>, onClick: (ToolbarButton) -> Unit = {}): List<ToolbarAction> =
    buttons.map { button -> ToolbarAction(ToolbarButtonItem.Builtin(button)) { onClick(button) } }
