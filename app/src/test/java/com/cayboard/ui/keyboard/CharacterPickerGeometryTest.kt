package com.cayboard.ui.keyboard
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test
class CharacterPickerGeometryTest {
    @Test fun everyPrintedCharacterAppearsOnce() {
        assertEquals(listOf("1", "q", "Q", "!"), keyCharacterChoices("q", "q", "1", "1", "!", emptyList()))
        assertEquals(listOf("1", "Q", "q", "!"), keyCharacterChoices("Q", "q", "1", "1", "!", emptyList()))
        assertEquals(listOf("7", "p", "q", "r", "s", "P", "Q", "R", "S"), keyCharacterChoices("pqrs", null, "7", "7", null, listOf("p", "q", "r", "s")))
        assertEquals(listOf("a", "s", "A", "S", "@", "+"), keyCharacterChoices("as", "as", null, "@", "+", emptyList()))
        assertEquals(listOf("1"), keyCharacterChoices("分词", null, "1", "1", null, emptyList()))
    }
    @Test fun edgeClampingDoesNotChangeHitCells() {
        for (key in listOf(Rect(0f, 220f, 40f, 280f), Rect(340f, 220f, 380f, 280f))) {
            val layout = characterPickerLayout(key, 380f, 1f, 5)
            assertTrue(layout.bounds.left >= 0f && layout.bounds.right <= 380f)
            repeat(5) { i ->
                val point = Offset(layout.bounds.left + (i + .5f) * layout.cellWidth, layout.bounds.center.y)
                assertEquals(i, layout.selection(point, 0, 5))
            }
            assertEquals(-1, layout.selection(Offset(-100f, -100f), 0, 5))
            assertEquals(0, layout.selection(key.center, 0, 5))
        }
    }
    @Test fun uppercaseRowsRemainSpaciousAndAboveTheirHeldKey() {
        val key = Rect(340f, 20f, 380f, 80f)
        val layout = characterPickerLayout(key, 380f, 1f, 9, minimumTop = -300f)
        assertEquals(5, layout.columns)
        assertEquals(60f, layout.cellWidth, .01f)
        assertEquals(64f, layout.cellHeight, .01f)
        assertTrue(layout.bounds.bottom < key.top)
        repeat(9) { i ->
            val point = Offset(layout.bounds.left + (i % layout.columns + .5f) * layout.cellWidth,
                layout.bounds.top + (i / layout.columns + .5f) * layout.cellHeight)
            assertEquals(i, layout.selection(point, 0, 9))
        }
        assertEquals(0, layout.selection(key.center, 0, 9))
        assertEquals(-1, layout.selection(Offset(layout.bounds.right - 1f, layout.bounds.bottom - 1f), 0, 9))
    }
}
