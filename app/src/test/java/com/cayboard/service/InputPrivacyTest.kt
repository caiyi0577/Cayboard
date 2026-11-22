package com.cayboard.service

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.cayboard.settings.SensitiveInputState
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputPrivacyTest {
    private val saved = Pair(SensitiveInputState.sensitiveEditor, SensitiveInputState.clipboardSearch)
    private val savedPasswordField = SensitiveInputState.passwordField
    private val privacy = InputPrivacy().also {
        SensitiveInputState.sensitiveEditor = false
        SensitiveInputState.clipboardSearch = false
    }

    @After fun restore() {
        SensitiveInputState.sensitiveEditor = saved.first
        SensitiveInputState.clipboardSearch = saved.second
        SensitiveInputState.passwordField = savedPasswordField
    }

    private fun field(inputType: Int, imeOptions: Int = 0) =
        EditorInfo().apply { this.inputType = inputType; this.imeOptions = imeOptions }
    private val text = InputType.TYPE_CLASS_TEXT
    private val password = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

    @Test fun normalFieldClearsSensitiveStatus() {
        privacy.beginEditor(field(text))
        privacy.beginEditor(field(text))
        privacy.beginEditor(field(password))
        privacy.beginEditor(field(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD))
        privacy.beginEditor(field(text, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING))
        assertTrue(SensitiveInputState.active)
        privacy.beginEditor(field(text))
        assertFalse(SensitiveInputState.active)
    }

    @Test fun searchOpenedInAPasswordFieldKeepsItPrivateAfterClosing() {
        privacy.beginEditor(field(password))
        privacy.setClipboardSearch(true)
        privacy.setClipboardSearch(false)
        assertTrue(SensitiveInputState.active)
    }

    @Test fun aFieldChangeDuringSearchIsNotUndoneWhenSearchCloses() {
        privacy.beginEditor(field(text))
        privacy.setClipboardSearch(true)
        privacy.beginEditor(field(password))
        privacy.setClipboardSearch(false)
        assertTrue(SensitiveInputState.active)
        // And back: a normal field learns again once search closes.
        privacy.setClipboardSearch(true)
        privacy.beginEditor(field(text))
        privacy.setClipboardSearch(false)
        assertFalse(SensitiveInputState.active)
    }

    /** 用户 11-09：网页上复制的普通文字被当成密码。剪贴板只认开着的藏字密码框。 */
    @Test fun clipboardCountsOnlyAnOpenHiddenPasswordField() {
        privacy.beginEditor(field(text, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING))
        assertTrue(SensitiveInputState.active)
        assertFalse(SensitiveInputState.passwordField)
        privacy.beginEditor(field(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD))
        assertFalse(SensitiveInputState.passwordField)
        privacy.beginEditor(field(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
        assertTrue(SensitiveInputState.passwordField)
        privacy.beginEditor(field(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD))
        assertTrue(SensitiveInputState.passwordField)
        privacy.beginEditor(field(password))
        assertTrue(SensitiveInputState.passwordField)
        // Leaving the field: a copy made on a web page afterwards is not from a password field.
        privacy.endEditor()
        assertFalse(SensitiveInputState.passwordField)
        assertTrue(SensitiveInputState.active)
    }
}
