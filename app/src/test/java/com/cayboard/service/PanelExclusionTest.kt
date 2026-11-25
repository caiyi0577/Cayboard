package com.cayboard.service

import androidx.compose.runtime.mutableStateOf
import com.cayboard.clipboard.ClipboardManager
import com.cayboard.rime.InputCommandQueue
import com.cayboard.settings.SensitiveInputState
import com.cayboard.viewmodel.KeyboardViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.*

/**
 * The clipboard edit form and clipboard search never stay open together. With both open the
 * letters went to search while chosen candidates filled the form, and closing the form reopened the
 * board under the search: two overlapping clipboards (user, 2026-10-23).
 */
class PanelExclusionTest {
    private val service = mock<CayboardInputMethodService>()
    private val uiState = mutableStateOf(InputUIState())

    init {
        val clipboard = mock<ClipboardManager>()
        whenever(clipboard.boardQuery).thenReturn(MutableStateFlow(""))
        mapOf(
            "uiState" to uiState, "clipboardManager" to clipboard,
            "candidateState" to mutableStateOf(CandidateState()),
            "inputCommands" to mock<InputCommandQueue>(), "privacy" to InputPrivacy(),
        ).forEach { (name, value) ->
            CayboardInputMethodService::class.java.getDeclaredField(name).apply {
                isAccessible = true
                set(service, value)
            }
        }
        whenever(service.uiState).thenReturn(uiState)
        whenever(service.clipboardManager).thenReturn(clipboard)
        whenever(service.keyboardViewModel).thenReturn(mock<KeyboardViewModel>())
        whenever(service.editorEnterKeyText()).thenReturn("发送")
        doCallRealMethod().whenever(service).openClipboardSearch()
        doNothing().whenever(service).fitClipboardSearchOnShortScreen()
        doCallRealMethod().whenever(service).closeClipboardSearch(any(), any())
    }

    @After fun restore() {
        SensitiveInputState.clipboardSearch = false
    }

    @Test fun openingSearchKeepsTheHostStateSeparate() {
        service.openClipboardSearch()
        assertEquals(InputTarget.ClipboardSearch, uiState.value.inputTarget)
        assertEquals("完成", uiState.value.enterKeyText)
    }
}
