package com.cayboard.ui.keyboard

import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.cayboard.ocr.OcrDraftSaveResult
import com.cayboard.service.OcrDraftEditTextHolder

/** A separate editor window above the keyboard; existing IME input still targets the draft. */
@Composable
fun OcrDraftPopup(
    backgroundColor: Color,
    textColor: Color,
    accentColor: Color,
    isFocused: Boolean,
    initialText: String = "",
    cardBgColor: Color,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    // Clipboard items no longer carry a trigger code; phrases with codes live in 自定义短语.
    onRetake: (() -> Unit)? = null,
) {
    // Drafts overlay other content, so they need their own opaque theme surface and matching text.
    val popupBackground = if (backgroundColor.alpha > 0f) backgroundColor else cardBgColor
    val popupText = textColor
    val currentOnFocusChange by rememberUpdatedState(onFocusChange)
    val density = LocalDensity.current
    val gap = with(density) { 8.dp.roundToPx() }
    val safeTop = WindowInsets.statusBars.getTop(density)
    var availableHeight by remember { mutableIntStateOf(0) }
    val position = remember(gap, safeTop) { object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
            layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
            // The anchor is the actual keyboard edge, including floating/landscape placement.
            val above = (anchorBounds.top - safeTop - gap * 2).coerceAtLeast(0)
            availableHeight = if (above > 0) above else (windowSize.height - safeTop - gap * 2).coerceAtLeast(1)
            return IntOffset(
            (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2)
                .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            (anchorBounds.top - popupContentSize.height - gap)
                .coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
            )
        }
    } }
    val maxHeight = if (availableHeight > 0) with(density) { availableHeight.toDp() }
        else (LocalConfiguration.current.screenHeightDp * .45f).dp
    // Short drafts wrap tightly; long drafts scroll only after using the free space above the IME.
    val editorMaxHeight = (maxHeight - 60.dp).coerceAtLeast(24.dp)
    // The popup is a sub-window where Android offers no text menu; the editor reports when the
    // footer should show selection actions instead.
    var selectionActions by remember(initialText) { mutableStateOf(false) }
    val selectableEditor = remember { arrayOfNulls<PopupSelectableEditText>(1) }
    val save: () -> Unit = {
        val editor = OcrDraftEditTextHolder.editText
        val text = editor?.text?.toString().orEmpty()
        if (text.isBlank()) {
            editor?.error = OcrDraftSaveResult.EMPTY_TEXT.errorMessage
            editor?.requestFocus()
        } else onSave(text)
    }
    BoxWithConstraints(modifier.fillMaxWidth().height(0.dp).testTag("ocr-review-anchor")) {
        val width = (maxWidth - 16.dp).coerceAtLeast(1.dp).coerceAtMost(640.dp)
        Popup(popupPositionProvider = position,
            properties = PopupProperties(focusable = false, dismissOnBackPress = false,
                dismissOnClickOutside = false, clippingEnabled = false)) {
            Column(Modifier.width(width).clip(RoundedCornerShape(16.dp))
                // Gradient/image themes paint the theme backdrop like the keyboard panels; a solid
                // theme keeps its colour. Never glass: the editor floats over the app without a
                // blur window, so it stays opaque and readable.
                .keyboardPanelBackground(popupBackground)
                .testTag("ocr-review-editor")
                .padding(12.dp)) {
                // Only a different record starts a new draft. Focus and theme recompositions must
                // not replace text or move the cursor back to the initial saved content.
                key(initialText) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AndroidView(
                            factory = { context ->
                                PopupSelectableEditText(context).apply {
                                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    showSoftInputOnFocus = false
                                    onSelectionActionsChanged = { selectionActions = it }
                                    selectableEditor[0] = this
                                    hint = "识别结果"
                                    textSize = 16f
                                    isSingleLine = false
                                    minLines = 1
                                    isVerticalScrollBarEnabled = true
                                    gravity = Gravity.TOP or Gravity.START
                                    setPadding(12, 8, 12, 8)
                                    setText(initialText)
                                    setSelection(initialText.length)

                                    imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION or
                                        EditorInfo.IME_ACTION_NONE

                                    onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                                        currentOnFocusChange(hasFocus)
                                    }
                                    setOnClickListener {
                                        currentOnFocusChange(true)
                                    }
                                    OcrDraftEditTextHolder.editText = this
                                    if (isFocused) {
                                        post { requestFocus() }
                                    }
                                }
                            },
                            update = { editText ->
                                editText.setTextColor(popupText.toArgb())
                                editText.setHintTextColor(popupText.copy(alpha = 0.4f).toArgb())
                                editText.handleColor = accentColor.toArgb()
                                editText.highlightColor = accentColor.copy(alpha = 0.3f).toArgb()
                            },
                            onReset = null,
                            onRelease = { editText ->
                                if (selectableEditor[0] === editText) selectableEditor[0] = null
                                if (OcrDraftEditTextHolder.editText === editText) {
                                    OcrDraftEditTextHolder.editText = null
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = minOf(32.dp, editorMaxHeight), max = editorMaxHeight),
                        )

                    }
                }
                Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    if (selectionActions) {
                        // Same footer height, so the draft never jumps while text is selected.
                        TextButton(onClick = { selectableEditor[0]?.selectAllText() }) { Text("全选", color = popupText) }
                        TextButton(onClick = { selectableEditor[0]?.copySelection() }) { Text("复制", color = popupText) }
                        TextButton(onClick = { selectableEditor[0]?.cutSelection() }) { Text("剪切", color = popupText) }
                        TextButton(onClick = { selectableEditor[0]?.pasteClipboard() }) { Text("粘贴", color = popupText) }
                    } else {
                        onRetake?.let { retake ->
                            TextButton(onClick = retake) { Text("重拍", color = popupText) }
                            Spacer(Modifier.weight(1f))
                        }
                        TextButton(onClick = onCancel) { Text("取消", color = popupText) }
                        TextButton(onClick = save) { Text("保存", color = accentColor) }
                    }
                }
            }
        }
    }
}
