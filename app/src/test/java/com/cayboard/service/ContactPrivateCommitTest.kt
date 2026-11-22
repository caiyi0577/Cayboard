package com.cayboard.service

import android.os.Handler
import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doCallRealMethod
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** 通讯录插件的人名、号码：上屏这一刻和之后都不能被学习（用户 11-04「一定要保证这个不能被学习到」）。 */
class ContactPrivateCommitTest {
    private val service = mock<CayboardInputMethodService>()
    private val prediction = mock<PredictionManager>()
    private val handler = mock<Handler>()
    private val readiness = InputReadiness().apply { completeStartup() }
    private val ui = mutableStateOf(InputUIState())
    private val candidates = mutableStateOf(CandidateState(associationCandidates = listOf("旧联想")))

    init {
        mapOf(
            "predictionManager" to prediction, "candidateState" to candidates,
            "mainHandler" to handler, "uiState" to ui, "inputReadiness" to readiness,
            "editorInputPolicy" to EditorSessionInputPolicy(),
        ).forEach { (name, value) ->
            CayboardInputMethodService::class.java.getDeclaredField(name).apply {
                isAccessible = true
                set(service, value)
            }
        }
        whenever(service.isChineseMode).thenReturn(true)
        whenever(service.commitTextSilently(any(), any(), any(), any())).thenReturn(TextCommitResult.ACCEPTED_HOST)
        doCallRealMethod().whenever(service).submitPrivateText(any(), any())
    }

    private fun <T> owned(block: () -> T): T =
        InputCommandOwner(readiness.ticket()!!, ui.value.inputSessionId) {}.runInline(block)

    @Test fun contactTextIsCommittedAsSensitiveAndClearsPredictions() {
        owned { service.submitPrivateText("张三") }
        // isSensitive = true：不发 text_committed、不记联想（commitTextSilently 的密码框分支）。
        verify(service).commitTextSilently("张三", false, true, CommitCaret.NONE)
        verify(service, never()).commitTextAndPredict(any(), any(), any(), any(), any())
        // 联想上下文清空：下一次上屏不会使用联系人姓名。
        verify(prediction).clearCommittedText()
        verify(prediction).invalidatePendingPredictions()
        // 上屏后不出联想。
        verify(handler, never()).post(any())
        assertTrue(candidates.value.associationCandidates.isEmpty())
    }

    @Test fun contactActionsAreMarkedPrivate() {
        assertTrue(CandidateAction.contact("13800138000").isPrivate)
        assertTrue(CandidateAction.contact("张三").isTextCandidate)
        assertTrue(!CandidateAction.text("张三").isPrivate)
    }
}
