package com.cayboard.ui.keyboard

import com.cayboard.settings.*
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberKeyboardSpacingTest {
    @Test fun numbersInheritTheActiveKeypadSettings() {
        fun profile(layout: InputLayout) = InputProfile(InputLanguage.CHINESE, InputScheme.PINYIN,
            layout, EngineProfile.Direct)
        assertEquals("t9", numberKeyboardSpacingSection(profile(InputLayout.T9), false))
        assertEquals("japanese_kana", numberKeyboardSpacingSection(profile(InputLayout.KANA), false))
        assertEquals("number", numberKeyboardSpacingSection(profile(InputLayout.T9), true))
        assertEquals("number", numberKeyboardSpacingSection(profile(InputLayout.QWERTY), false))
    }
}
