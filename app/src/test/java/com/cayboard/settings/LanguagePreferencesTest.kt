package com.cayboard.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguagePreferencesTest {
    @Test fun retiredNativeSelectionsFallBackToBundledChinese() {
        assertEquals("rime_ice", LanguagePreferences.nativeSchema(
            "rime_ice", listOf("rime_ice", "japanese_kana"),
            setOf(InputLanguage.JAPANESE, InputLanguage.ENGLISH)))
        assertEquals("rime_ice", LanguagePreferences.nativeSchema(
            "japanese", listOf("rime_ice", "japanese_kana"),
            setOf(InputLanguage.JAPANESE, InputLanguage.ENGLISH)))
        assertEquals("rime_ice", LanguagePreferences.nativeSchema(
            "japanese", listOf("rime_ice", "japanese", "japanese_kana"),
            setOf(InputLanguage.JAPANESE, InputLanguage.ENGLISH)))
    }

    @Test fun retiredSchemasDoNotEnableTheirLanguage() {
        val fresh = setOf(InputLanguage.CHINESE, InputLanguage.ENGLISH)
        assertEquals(fresh, LanguagePreferences.withSchemaLanguage(fresh, "japanese"))
        assertEquals(fresh, LanguagePreferences.withSchemaLanguage(fresh, "japanese_kana"))
        // A Chinese schema or an already enabled language changes nothing.
        assertEquals(fresh, LanguagePreferences.withSchemaLanguage(fresh, "pinyin_14jian"))
    }

    @Test fun englishOnlyKeepsRealNativeFallback() {
        assertEquals("rime_ice", LanguagePreferences.nativeSchema(
            InputModes.ENGLISH, listOf("rime_ice", "japanese_kana"), setOf(InputLanguage.ENGLISH)))
        assertEquals("rime_ice", LanguagePreferences.nativeSchema(
            "rime_ice", emptyList(), setOf(InputLanguage.ENGLISH)))
    }
}
