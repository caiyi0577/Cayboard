package com.cayboard.audit

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.cayboard.settings.KeysConfigHelper
import com.cayboard.ui.keyboard.AppFonts
import com.cayboard.ui.keyboard.KeyboardCallbacks
import com.cayboard.ui.keyboard.KeyboardDispatchAction
import com.cayboard.ui.keyboard.KeyboardView
import com.cayboard.ui.theme.KeyboardThemes
import com.cayboard.viewmodel.KeyboardUiState
import com.cayboard.viewmodel.KeyboardViewModel

/** Device probe runs the normal KeyboardView with private temporary preferences. */
@Composable
internal fun FloatingFooterScene(app: Application, record: (String) -> Unit, atBottom: Boolean, dark: Boolean) {
    val vm = remember {
        KeysConfigHelper.loadConfig(app)
        KeyboardThemes.initFromConfig(app)
        AppFonts.initialize(app)
        KeyboardViewModel(app).also { it.dispatch(KeyboardDispatchAction.InputSessionStarted(false, "cayboard_pinyin")) }
    }
    val config = LocalConfiguration.current
    val wide = minOf(config.screenWidthDp, config.screenHeightDp) >= 600
    com.cayboard.ui.theme.CayboardTheme(darkTheme = dark) {
        KeyboardView(vm, KeyboardUiState(isFloatingMode = true, keyboardWidthDp = if (wide) 560 else 320,
            keyboardHeightDp = 244, floatingOffsetY = if (atBottom) 0 else 80, floatingMinOffsetY = 0,
            currentSchemaId = "cayboard_pinyin", isDarkTheme = dark),
            KeyboardCallbacks(onKeyPress = { key, _ -> record(key) }, onCandidateSelect = {},
                onFloatingKeyboardDrag = { _, _ -> record("move") }), inputSessionManagedByHost = true)
    }
}
