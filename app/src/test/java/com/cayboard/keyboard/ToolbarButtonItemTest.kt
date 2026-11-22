package com.cayboard.keyboard

import org.junit.Assert.*
import org.junit.Test

class ToolbarButtonItemTest {
    @Test fun builtinsResolveWithoutAnyRuntimeRegistry() {
        assertEquals(ToolbarButton.EMOJI, (resolveToolbarButtonItem("emoji") as ToolbarButtonItem.Builtin).button)
        assertEquals(ToolbarButton.TRANSLATE, (resolveToolbarButtonItem("translate") as ToolbarButtonItem.Builtin).button)
    }
    @Test fun retiredAndUnknownActionsCannotBeResolved() {
        assertNull(resolveToolbarButtonItem("glass"))
        assertNull(resolveToolbarButtonItem("com.example.plugin:open"))
    }
}
