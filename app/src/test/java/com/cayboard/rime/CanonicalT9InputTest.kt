package com.cayboard.rime

import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalT9InputTest {
    @Test fun mapsPhysicalLettersAndScreenDigitsToSameCode() {
        assertEquals("333", canonicalT9Input("def"))
        assertEquals("3", canonicalT9Input("3"))
        assertEquals("3", canonicalT9Input("e"))
        assertEquals("54482", canonicalT9Input("jihua"))
        assertEquals("54482", canonicalT9Input("54482"))
    }

    @Test fun keepsOnlySupportedT9SeparatorsAndKeys() {
        assertEquals("54482", canonicalT9Input("Ji'Hua"))
        assertEquals("", canonicalT9Input("!@#"))
    }
}
