package com.cayboard.ui.keyboard

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeBubbleControllerTest {
    private val bounds = Rect(10f, 20f, 70f, 80f)

    @Test fun idleDoesNotPublishKeyBounds() {
        val controller = SwipeBubbleController()
        controller.update(SwipeState(), bounds)
        assertEquals(SwipeState(), controller.state)
        assertEquals(Rect.Zero, controller.keyBounds)
    }

    @Test fun swipeAndLongPressChoicesEndAtRelease() {
        val controller = SwipeBubbleController()
        controller.update(SwipeState(isSwiping = true, swipeText = "1"), bounds)
        assertEquals("1", controller.state.swipeText)
        assertEquals(bounds, controller.keyBounds)
        controller.update(SwipeState(), bounds)
        assertEquals(SwipeState(), controller.state)

        controller.update(SwipeState(isLongPress = true, longPressItems = listOf("a", "A")), bounds)
        assertTrue(controller.state.isLongPress)
        controller.update(SwipeState(), bounds)
        assertEquals(SwipeState(), controller.state)
    }

    @Test fun inlineFlickDoesNotAlsoShowAnOverlay() {
        val controller = SwipeBubbleController()
        val swipe = SwipeState(isSwiping = true, swipeText = "1")
        controller.update(swipe, bounds)
        controller.update(swipe.copy(keyFlick = KeyFlickPreview("1", 1f, true)), bounds)
        assertEquals(SwipeState(), controller.state)
    }
}
