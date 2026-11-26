package com.cayboard.ui.keyboard
import com.cayboard.keyboard.ToolbarButton
import com.cayboard.settings.LayoutKind
import org.junit.Assert.*
import org.junit.Test
class LayoutGestureMenuItemsTest {
    @Test fun removedActionsDisappearFromSavedAndCustomizableMenus() {
        assertNull(ToolbarButton.fromId("t9_flick"))
        assertNull(ToolbarButton.fromId("flick_disc"))
        assertNull(ToolbarButton.fromId("kana_hints"))
        assertEquals(listOf("clipboard"), ToolbarButton.normalizeIds(listOf("t9_flick", "clipboard", "flick_disc")))
        assertTrue(ToolbarButton.CUSTOMIZABLE.none { it.id in setOf("t9_flick", "flick_disc") })
    }
    @Test fun retainedGesturesStayAvailable() {
        assertEquals(listOf(ToolbarButton.FOURTEEN_SWIPE), ToolbarButton.LAYOUT_GESTURES.filter { it.appliesTo(LayoutKind.MERGED) })
        assertTrue(ToolbarButton.LAYOUT_GESTURES.none { it.id.contains("glide") })
    }
}
