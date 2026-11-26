package com.cayboard.ui.render

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.ui.keyboard.KeyboardCallbacks
import com.cayboard.ui.keyboard.KeyboardView
import com.cayboard.viewmodel.KeyboardUiState
import com.cayboard.viewmodel.KeyboardViewModel
import com.cayboard.engine.T9InputController
import com.cayboard.settings.KeysConfigHelper
import com.cayboard.ui.keyboard.AppFonts
import com.cayboard.ui.theme.KeyboardThemes
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class GlideCandidatePreviewRenderTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun glideCandidatePreviewDisplaysEngineCandidatesWithoutChangingTheInputState() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        KeysConfigHelper.loadConfig(app)
        KeyboardThemes.initFromConfig(app)
        AppFonts.initialize(app)
        val vm = spy(KeyboardViewModel(app))
        var selections = 0
        val callbacks = KeyboardCallbacks(onKeyPress = { _, _ -> }, onCandidateSelect = { selections++ })
        doReturn(mock<T9InputController>()).whenever(vm).bindT9Controller(callbacks)
        val original = mutableStateOf(com.cayboard.service.CandidateState())
        val preview = mutableStateOf<com.cayboard.engine.EngineProcessResult?>(
            com.cayboard.engine.EngineProcessResult(true, "", "nihao", "ni hao",
                arrayOf(com.cayboard.engine.EngineCandidate("你好", "ni hao")), false, false, false))
        rule.setContent { MaterialTheme {
            KeyboardView(vm, KeyboardUiState(), callbacks, Modifier.width(360.dp).height(300.dp),
                candidateState = original, glideEnginePreview = preview, inputSessionManagedByHost = true)
        } }
        rule.onNodeWithText("你好").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(0, selections); assertEquals("", original.value.inputText); preview.value = null }
        rule.onNodeWithText("你好").assertDoesNotExist()
    }

}
