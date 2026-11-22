package com.cayboard.ocr

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cayboard.clipboard.ClipboardManager
import com.cayboard.ui.keyboard.OcrDraftPopup

internal fun saveOcrDraft(context: Context, text: String): OcrDraftSaveResult {
    if (text.isBlank()) return OcrDraftSaveResult.EMPTY_TEXT
    val manager = ClipboardManager.getInstance(context)
    manager.copyToSystemClipboard(text)
    manager.addItem(text)
    return OcrDraftSaveResult.ACCEPTED
}

/** The same popup and input routing as clipboard editing; only the draft's save destination differs. */
@Composable
internal fun OcrReviewEditor(text: String, textColor: Color, background: Color, accent: Color,
    onFocus: (Boolean) -> Unit, onSave: (String) -> Unit, onCancel: () -> Unit, onRetake: () -> Unit) {
    OcrDraftPopup(backgroundColor = Color.Transparent, textColor = textColor, accentColor = accent,
        isFocused = true, initialText = text, cardBgColor = background,
        onSave = onSave, onCancel = onCancel,
        onFocusChange = onFocus, onRetake = onRetake)
}
