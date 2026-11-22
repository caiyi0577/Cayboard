package com.cayboard.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cayboard.gloss.CandidateGlossary
import com.cayboard.model.ModelDownloadState
import com.cayboard.model.ModelManager

/**
 * 从安装包准备内置释义词典的状态。
 * [startRequest] 每次递增都会尝试准备词典（用户刚选“释义”时由调用方触发）。
 */
@Composable
internal fun GlossDictionaryStatus(startRequest: Int) {
    val context = LocalContext.current
    val states by ModelManager.downloadStates.collectAsState()
    val state = states[CandidateGlossary.MODEL_ID]
    var installed by remember { mutableStateOf(CandidateGlossary.isInstalled(context)) }
    val start = {
        ModelManager.getModel(CandidateGlossary.MODEL_ID)?.let { ModelManager.downloadModelInBackground(context, it) }
        Unit
    }
    LaunchedEffect(state) {
        if (state is ModelDownloadState.Complete) {
            installed = CandidateGlossary.isInstalled(context)
            CandidateGlossary.prepare(context)
        }
    }
    LaunchedEffect(startRequest) {
        if (startRequest > 0 && !installed && state !is ModelDownloadState.Downloading) start()
    }
    if (installed) return

    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp).testTag("gloss-dictionary-status"),
        verticalAlignment = Alignment.CenterVertically) {
        Text(
            when (state) {
                is ModelDownloadState.Downloading ->
                    "正在准备内置释义词典…"
                is ModelDownloadState.Error -> "内置释义词典准备失败：${state.message}"
                else -> "释义词典尚未准备好，暂时显示拼音。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (state is ModelDownloadState.Downloading) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        if (state !is ModelDownloadState.Downloading) {
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = start, modifier = Modifier.testTag("gloss-dictionary-prepare")) {
                Text(if (state is ModelDownloadState.Error) "重试" else "准备词典")
            }
        }
    }
}
