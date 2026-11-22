package com.cayboard.ui.keyboard

import android.graphics.Rect
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.positionOnScreen
import com.cayboard.service.HardwareCursorAnchor
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.semantics.selected
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.floor
import kotlin.math.ceil

/** The full-screen host has no input handler: only this capsule enters the IME touchable region. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun HardwareKeyboardToolbar(
    onShowKeyboard: () -> Unit,
    onEmoji: () -> Unit,
    onClipboard: () -> Unit,
    onSwitchLanguage: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    onBoundsChanged: (Rect?) -> Unit,
    position: HardwareToolbarPosition,
    onPositionChange: (HardwareToolbarPosition) -> Unit,
    cursorAnchor: HardwareCursorAnchor? = null,
    editorBounds: HardwareCursorAnchor? = null,
    avoidEditor: Boolean = true,
    languageActions: KeyboardInputActions = KeyboardInputActions(),
    surface: (@Composable () -> Modifier)? = null,
    languageLabel: String = "中",
    selectedTextColor: Color = contentColor,
    accentColor: Color = contentColor,
    onVoice: (() -> Unit)? = null,
    voiceActive: Boolean = false,
    statusText: String? = null,
    capsLock: Boolean = false,
) {
    val themeKey = com.cayboard.ui.theme.LocalKeyboardPalette.current?.key ?: backgroundColor
    val density = LocalDensity.current
    val shadowRadius = with(density) { 1.dp.toPx() }

    val foreground = contentColor
    val reportBounds by rememberUpdatedState(onBoundsChanged)
    val savePosition by rememberUpdatedState(onPositionChange)
    val currentPosition by rememberUpdatedState(position)
    DisposableEffect(Unit) { onDispose { reportBounds(null) } }
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val languageMenuActive = LocalSuppressCursorMove.current
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { hostOrigin = it.positionOnScreen() },
        contentAlignment = AbsoluteAlignment.TopLeft) {
        val width = minOf(56.dp, maxWidth)
        val padding = minOf(4.dp, maxHeight / 20)
        val handleHeight = minOf(32.dp, maxHeight / 6)
        val actionCount = if (onVoice == null) 4 else 5
        val buttonSize = minOf(48.dp, (maxHeight - padding * 2 - handleHeight) / actionCount,
            (width - padding * 2).coerceAtLeast(0.dp)).coerceAtLeast(0.dp)
        val height = buttonSize * actionCount + handleHeight + padding * 2
        val widthPx = with(density) { width.roundToPx() }
        val heightPx = with(density) { height.roundToPx() }
        val geometry = hardwareToolbarGeometry(constraints.maxWidth, constraints.maxHeight, widthPx, heightPx,
            with(density) { 8.dp.roundToPx() })
        val currentGeometry by rememberUpdatedState(geometry)
        val gap = with(density) { 12.dp.roundToPx() }
        val editor = editorBounds?.takeIf {
            (it.right - it.left) * (it.bottom - it.top) < constraints.maxWidth.toFloat() * constraints.maxHeight * 0.6f
        } ?: cursorAnchor?.let { HardwareCursorAnchor(it.left - gap * 3, it.top, it.right + gap * 3, it.bottom) }
        val exclusion = editor?.let { HardwareCandidateExclusion(
            (it.left - hostOrigin.x).roundToInt(), (it.top - hostOrigin.y).roundToInt(),
            (it.right - hostOrigin.x).roundToInt(), (it.bottom - hostOrigin.y).roundToInt()) }
        val preferred = geometry.offset(position.sideDocked())
        val resting = if (avoidEditor) avoidHardwareSideEditor(geometry, preferred, widthPx, heightPx, exclusion, gap) else preferred
        var dragging by remember { mutableStateOf<HardwareToolbarOffset?>(null) }
        var gestureOrigin by remember { mutableStateOf<HardwareToolbarOffset?>(null) }
        val animated by animateOffsetAsState((dragging ?: resting).let { Offset(it.x, it.y) },
            if (dragging != null) snap() else tween(220), label = "hardware-side-position")
        val displayed = dragging?.let { geometry.constrain(it.x, it.y) } ?: geometry.constrain(animated.x, animated.y)
        val currentDisplayed by rememberUpdatedState(displayed)
        val shape = RoundedCornerShape(HardwareCandidateCornerRadius)
        Box(Modifier.absoluteOffset {
            val origin = gestureOrigin ?: displayed
            IntOffset(origin.x.roundToInt(), origin.y.roundToInt())
        }.width(width).height(height).testTag("hardware-toolbar-drag").semantics {
            contentDescription = "移动输入工具条"
            customActions = listOf(
                CustomAccessibilityAction("靠左") { savePosition(currentPosition.sideDocked().copy(xFraction = 0f)); true },
                CustomAccessibilityAction("靠右") { savePosition(currentPosition.sideDocked().copy(xFraction = 1f)); true })
        }.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val origin = currentDisplayed
                gestureOrigin = origin
                var moved = false
                try {
                    while (true) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                        val distance = change.position - down.position
                        if (!languageMenuActive.value && !change.isConsumed && (moved || distance.getDistance() > touchSlop)) {
                            moved = true
                            dragging = currentGeometry.constrain(origin.x + distance.x, origin.y + distance.y)
                            change.consume()
                        }
                        if (!change.pressed) {
                            if (moved) savePosition(currentGeometry.sidePositionAt(origin.x + distance.x, origin.y + distance.y, currentPosition))
                            break
                        }
                    }
                } finally { dragging = null; gestureOrigin = null }
            }
        }) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(Modifier.absoluteOffset {
                    val origin = gestureOrigin ?: displayed
                    IntOffset((displayed.x - origin.x).roundToInt(), (displayed.y - origin.y).roundToInt())
                }.width(width).height(height).onGloballyPositioned {
                    val b = it.boundsInWindow()
                    reportBounds(Rect(floor(b.left).toInt(), floor(b.top).toInt(), ceil(b.right).toInt(), ceil(b.bottom).toInt()))
                }.clip(shape).then(surface?.invoke() ?: Modifier.background(backgroundColor))
                    .testTag("hardware-keyboard-toolbar").padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(buttonSize).height(handleHeight).testTag("hardware-toolbar-handle"), contentAlignment = Alignment.Center) {
                        if (statusText != null) androidx.compose.material3.CircularProgressIndicator(
                            Modifier.size(14.dp).semantics { contentDescription = statusText }, color = foreground, strokeWidth = 2.dp)
                        else Icon(Icons.Default.DragIndicator, null, tint = foreground, modifier = Modifier.size(20.dp))
                    }
                    HardwareToolbarAction(Icons.Default.Keyboard, "展开屏幕键盘", "hardware-toolbar-keyboard", foreground, buttonSize, onClick = onShowKeyboard)
                    HardwareToolbarAction(Icons.Default.EmojiEmotions, "表情", "hardware-toolbar-emoji", foreground, buttonSize, onClick = onEmoji)
                    HardwareToolbarAction(Icons.Default.ContentPaste, "剪贴板", "hardware-toolbar-clipboard", foreground, buttonSize, onClick = onClipboard)
                    onVoice?.let { HardwareToolbarAction(Icons.Default.Mic, "语音输入", "hardware-toolbar-voice", foreground, buttonSize,
                        active = voiceActive, onClick = it) }
                    HardwareToolbarLanguageKey(foreground, languageActions, languageLabel, onSwitchLanguage, buttonSize, capsLock, accentColor)
                }
            }
        }
        if (dragging != null) {
            val dockTarget = geometry.sidePositionAt(displayed.x, displayed.y, position)
            val preview = geometry.offset(dockTarget)
            Box(Modifier.absoluteOffset { IntOffset(preview.x.roundToInt(), preview.y.roundToInt()) }
                .width(width).height(height).border(2.dp, Color(0xFF64B5F6), shape).testTag("hardware-toolbar-dock-preview"))
        }
    }
}

/** Like Gboard's pill: a plain icon in a full-size touch target; the circle shows only while pressed or on. */
@Composable
internal fun HardwareToolbarAction(
    icon: ImageVector,
    description: String,
    tag: String,
    contentColor: Color,
    size: Dp = 48.dp,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        Modifier.size(size).clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description; selected = active }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(minOf(36.dp, size)).clip(CircleShape)
            .background(contentColor.copy(alpha = if (active) .22f else if (pressed) .14f else 0f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(minOf(20.dp, size)))
        }
    }
}

@Composable
internal fun HardwareToolbarLanguageKey(
    color: Color, actions: KeyboardInputActions, label: String, onClick: () -> Unit, size: Dp = 48.dp,
    capsLock: Boolean = false, capsColor: Color = color,
) {
    androidx.compose.runtime.CompositionLocalProvider(LocalKeyboardInputActions provides actions) {
        Box(Modifier.size(size).semantics(mergeDescendants = true) {
            contentDescription = "当前语言：$label${if (capsLock) "，大写锁定已开启" else ""}，点按切换，长按选择语言"
        }.testTag("hardware-toolbar-language")) {
            LanguageKeyButton(onClick = onClick, backgroundColor = Color.Transparent, textColor = color,
                languageLabel = label, fontSize = 18.sp, shadowEnabled = false, shadowShapeRadius = 24.dp)
            // Caps Lock 指示叠在语言键右上角，不占候选区宽度。
            if (capsLock) androidx.compose.material3.Text("⇪", Modifier.align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 4.dp).testTag("hardware-toolbar-caps"),
                color = capsColor, fontSize = 12.sp, lineHeight = 12.sp)
        }
    }
}

