package com.cayboard.service

import android.view.inputmethod.EditorInfo
import com.cayboard.settings.SensitiveInputState

internal class InputPrivacy {
    /** A new field: password fields and fields asking for no personalized learning stay private. */
    fun beginEditor(attribute: EditorInfo?) {
        SensitiveInputState.sensitiveEditor = SensitiveInputState.isSensitiveEditor(attribute)
        SensitiveInputState.passwordField = SensitiveInputState.isPasswordField(attribute)
    }

    /** The field closed: a copy made after this did not come from a password field. */
    fun endEditor() { SensitiveInputState.passwordField = false }

    /** Its own source: a field change while search is open must not overwrite it, nor be restored by it. */
    fun setClipboardSearch(open: Boolean) { SensitiveInputState.clipboardSearch = open }
}
