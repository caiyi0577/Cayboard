package com.cayboard.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguagePreferencesTest {
    @Test fun retiredNativeSelectionsFallBackToBundledChinese() {
        assertEquals("cayboard_pinyin", LanguagePreferences.nativeSchema(
            "cayboard_pinyin", listOf("cayboard_pinyin", "japanese_kana"),
            setOf(InputLanguage.JAPANESE, InputLanguage.ENGLISH)))
        assertEquals("cayboard_pinyin", LanguagePreferences.nativeSchema(
            "japanese", listOf("cayboard_pinyin", "japanese_kana"),
            setOf(InputLanguage.JAPANESE, InputLanguage.ENGLISH)))
        assertEquals("cayboard_pinyin", LanguagePreferences.nativeSchema(
            "japanese", listOf("cayboard_pinyin", "japanese", "japanese_kana"),
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
        assertEquals("cayboard_pinyin", LanguagePreferences.nativeSchema(
            InputModes.ENGLISH, listOf("cayboard_pinyin", "japanese_kana"), setOf(InputLanguage.ENGLISH)))
        assertEquals("cayboard_pinyin", LanguagePreferences.nativeSchema(
            "cayboard_pinyin", emptyList(), setOf(InputLanguage.ENGLISH)))
    }
}
