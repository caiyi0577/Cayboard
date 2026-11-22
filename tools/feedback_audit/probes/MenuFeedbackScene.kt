package com.cayboard.audit

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cayboard.keyboard.ToolbarAction
import com.cayboard.keyboard.ToolbarButton
import com.cayboard.keyboard.ToolbarButtonItem
import com.cayboard.model.ModelDownloadState
import com.cayboard.settings.InputLanguage
import com.cayboard.speech.SpeechModelCatalog
import com.cayboard.ui.keyboard.*
import com.cayboard.ui.settings.SpeechModelChoices
import com.cayboard.ui.theme.KeyboardThemes
import com.cayboard.ui.theme.CayboardTheme

/** Render the actual settings list and candidate bar. Download actions use a deterministic transport. */
@Composable
internal fun MenuFeedbackScene(app: Application, record: (String) -> Unit, mode: String) {
    remember { KeyboardThemes.initFromConfig(app); AppFonts.initialize(app) }
    CayboardTheme(darkTheme = true) {
        Surface(Modifier.fillMaxSize()) {
            if (mode.endsWith("speech")) {
                val original = SpeechModelCatalog.ZIPFORMER_JA
                val light = SpeechModelCatalog.ZIPFORMER_JA_INT8
                var selected by remember { mutableStateOf(original) }
                val downloads = remember { mutableStateMapOf<String, ModelDownloadState>() }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(Modifier.width(280.dp).padding(16.dp)) {
                        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.4f)) {
                            Text("日语主模型", style = MaterialTheme.typography.headlineSmall)
                            SpeechModelChoices(InputLanguage.JAPANESE, selected, setOf(light, SpeechModelCatalog.SENSEVOICE), downloads,
                                onSelect = { selected = it; record("select:$it") },
                                onDownload = {
                                    record("download:$it")
                                    downloads[it] = if (downloads[it] == null) ModelDownloadState.Error("连接中断")
                                        else ModelDownloadState.Downloading(.42f, 42, 100)
                                })
                        }
                    }
                }
            } else {
                val buttons = ToolbarButton.entries.take(4).map { ToolbarAction(ToolbarButtonItem.Builtin(it)) {} }
                Column(Modifier.fillMaxWidth().wrapContentHeight(Alignment.CenterVertically)) {
                    for (state in listOf(CandidateBarState.Idle, CandidateBarState.ChineseCandidates(
                        listOf("你好", "你", "拟"), inputText = "nihao", hasMore = true))) {
                        CandidateBar(state, toolbarActions = buttons, showPreeditPreview = false,
                            visuals = CandidateBarVisuals(Color.DarkGray, Color.White, Color.Gray, isDarkTheme = true),
                            callbacks = CandidateBarCallbacks(onCandidateSelect = {}, onLogoClick = {}, onHideKeyboard = { record("hide") },
                                onShowMoreCandidates = { record("expand") }, onCancelInput = {}),
                            modifier = Modifier.fillMaxWidth().height(44.dp))
                    }
                }
            }
        }
    }
}
