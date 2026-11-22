package com.cayboard.ui.keyboard

import com.cayboard.ui.theme.selectionColors
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cayboard.settings.ClipboardWordSegmenter
import com.cayboard.viewmodel.KeyboardViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SplitWordsView(
    text: String, backgroundColor: Color, viewModel: KeyboardViewModel,
    onBack: () -> Unit,
    onConfirmText: (String) -> Unit, bottomPaddingDp: Int = 0,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val selection = selectionColors()
    val colors = MaterialTheme.colorScheme
    val chipColor = lerp(colors.surface, colors.primary, if (colors.surface.luminance() < .5f) .10f else .065f)
    var words by remember(text) { mutableStateOf(ClipboardWordSegmenter.quickSplit(text)) }
    var characters by remember(text) { mutableStateOf<List<String>?>(null) }
    var characterMode by remember(text) { mutableStateOf(false) }
    var loading by remember(text) { mutableStateOf(true) }
    var failed by remember(text) { mutableStateOf(false) }
    LaunchedEffect(text) {
        try { words = ClipboardWordSegmenter.split(context, text) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true; words = ClipboardWordSegmenter.characters(text) }
        finally { loading = false }
    }
    LaunchedEffect(text, characterMode) {
        if (characterMode && characters == null) characters = withContext(Dispatchers.Default) { ClipboardWordSegmenter.characters(text) }
    }
    val parts = if (characterMode) characters ?: words else words
    var selected by remember(parts, characterMode) { mutableStateOf(emptySet<Int>()) }
    val selectedText = remember(parts, selected) { selected.sorted().joinToString("") { parts.getOrNull(it).orEmpty() } }
    val ready = if (characterMode) characters != null else !loading
    val chunks = remember(parts) { parts.chunked(24) }
    val listState = rememberLazyListState()
    val chipBounds = remember(parts) { mutableMapOf<Int, Rect>() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    fun togglePart(index: Int) { selected = if (index in selected) selected - index else selected + index }
    fun findChip(position: Offset) = chipBounds.entries.firstOrNull { it.value.contains(position + origin) }?.key

    Column(modifier.fillMaxWidth().keyboardPanelBackground(backgroundColor).padding(bottom = bottomPaddingDp.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 6.dp).clipboardPanelExpandGesture(),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = colors.onSurface, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(start = 6.dp)) {
                Text("拆词", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                Text(if (selected.isEmpty()) "点选或横向滑动连选" else "已选 ${selected.size} 项 · ${selectedText.codePointCount(0,selectedText.length)} 字",
                    fontSize = 11.sp, color = colors.onSurfaceVariant, maxLines = 1)
            }
            IconButton(enabled = selectedText.isNotEmpty(), onClick = {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Cayboard 拆词", selectedText))
                Toast.makeText(context, "已复制所选文字", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.testTag("split-copy")) { Icon(Icons.Default.ContentCopy, "复制所选", tint = colors.onSurface.copy(alpha = if (selectedText.isNotEmpty()) 1f else .35f), modifier = Modifier.size(19.dp)) }
            FilledTonalButton(enabled = selectedText.isNotEmpty(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.primaryContainer, contentColor = colors.primary), contentPadding = PaddingValues(horizontal = 12.dp),
                onClick = { onConfirmText(selectedText); onBack() }, modifier = Modifier.height(32.dp).testTag("split-confirm")) {
                Text("确定", fontSize = 13.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(false to "词语", true to "逐字").forEach { (mode, label) ->
                Box(Modifier.clip(RoundedCornerShape(8.dp))
                    .background(if (characterMode == mode) selection.quietFill else Color.Transparent)
                    .semantics { this.selected = characterMode == mode }
                    .clickable(role = Role.Tab) { characterMode = mode }
                    .testTag("split-mode:$mode").padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(label, color = if (characterMode == mode) selection.quietContent else colors.onSurfaceVariant,
                        fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(enabled = ready && parts.isNotEmpty(), onClick = {
                selected = if (selected.size == parts.size) emptySet() else parts.indices.toSet()
            }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(32.dp).testTag("split-select-all")) {
                Text(if (selected.size == parts.size && parts.isNotEmpty()) "清空" else "全选", fontSize = 12.sp)
            }
            TextButton(enabled = ready, onClick = { selected = parts.indices.filterNot { it in selected }.toSet() },
                contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(32.dp).testTag("split-invert")) { Text("反选", fontSize = 12.sp) }
        }
        if (loading && !characterMode) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
        if (failed && !characterMode) Text("暂时使用逐字拆分，仍可选择文字", color = colors.onSurfaceVariant,
            fontSize = 11.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp))
        if (selectedText.isNotEmpty()) Text(selectedText.replace('\n',' '), maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = colors.primary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).testTag("split-selection-preview"))
        LazyColumn(state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("split-words-region")
                .onGloballyPositioned { origin = it.positionInRoot() }
                .pointerInput(parts, characterMode, ready) {
                    if (!ready) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val first = findChip(down.position)
                        val removing = first != null && first in selected
                        var mode = SplitGesture.PENDING
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (mode == SplitGesture.PENDING && first != null && !change.isConsumed)
                                    selected = if (removing) selected - first else selected + first
                                break
                            }
                            if (mode == SplitGesture.PENDING) {
                                if (change.isConsumed) { mode = SplitGesture.SCROLL; continue }
                                val moved = change.position - down.position
                                if (moved.getDistance() <= viewConfiguration.touchSlop) continue
                                mode = splitGestureMode(first != null, moved.x, moved.y, listState.canScrollForward || listState.canScrollBackward)
                            }
                            if (mode != SplitGesture.SELECT) continue
                            change.consume()
                            val at = findChip(change.position) ?: continue
                            val range = (minOf(first!!,at)..maxOf(first,at)).toSet()
                            selected = if (removing) selected - range else selected + range
                        }
                    }
                },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(chunks) { chunk, items ->
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.forEachIndexed { offset, part ->
                        val index = chunk * 24 + offset
                        val chosen = index in selected
                        DisposableEffect(chipBounds, index) { onDispose { chipBounds.remove(index) } }
                        Box(Modifier.testTag("split-part:$index")
                            .semantics { this.selected = chosen; role = Role.Checkbox; onClick { if (ready) togglePart(index); ready } }
                            .onGloballyPositioned { chipBounds[index] = it.boundsInRoot() }
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (chosen) selection.fill else chipColor)
                            .padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(splitPartLabel(part), color = if (chosen) selection.content else colors.onSurface,
                                fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

internal enum class SplitGesture { PENDING, SELECT, SCROLL }

/**
 * What a drag past the touch slop does: sideways from a word selects a run of words; up or down
 * scrolls the list and selects nothing, unless the list cannot scroll, where it still selects.
 */
internal fun splitGestureMode(onChip: Boolean, dx: Float, dy: Float, canScroll: Boolean): SplitGesture = when {
    !onChip -> SplitGesture.SCROLL
    canScroll && kotlin.math.abs(dy) >= kotlin.math.abs(dx) -> SplitGesture.SCROLL
    else -> SplitGesture.SELECT
}

private val NO_BREAK_SPACE = Char(0x00A0)

/** 空白词块按原样占宽显示；普通空格换成不换行空格，避免被当作行尾空白而不计宽度。 */
internal fun splitPartLabel(part: String): String = when {
    !part.isBlank() -> part
    part.contains('\n') -> "换行"
    else -> part.replace(' ', NO_BREAK_SPACE).replace("\t", NO_BREAK_SPACE.toString().repeat(4))
}
