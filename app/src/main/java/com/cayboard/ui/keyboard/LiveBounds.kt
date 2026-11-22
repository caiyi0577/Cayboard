package com.cayboard.ui.keyboard

import android.os.SystemClock
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import com.cayboard.util.FileLogger
import kotlin.math.abs

/**
 * 用当前未裁切的根布局坐标定位手势浮窗。节点未摆放或已拆下时使用缓存。
 */
internal class LiveBounds {
    var coordinates: LayoutCoordinates? = null

    fun current(cached: Rect, what: String): Rect {
        val node = coordinates?.takeIf { it.isAttached } ?: return cached
        val live = node.findRootCoordinates().localBoundingBoxOf(node, clipBounds = false)
        if (cached != Rect.Zero && !live.near(cached)) logMismatch(what, cached, live)
        return live
    }
}

private fun Rect.near(other: Rect) = abs(left - other.left) <= 1f && abs(top - other.top) <= 1f &&
    abs(right - other.right) <= 1f && abs(bottom - other.bottom) <= 1f

private const val TAG = "LiveBounds"
private const val LOG_INTERVAL_MS = 2_000L
private var lastLogAt = 0L

/** 记下的位置和现量的对不上：写进日志（关于 → 日志查看器能看到），两秒最多一条。 */
private fun logMismatch(what: String, cached: Rect, live: Rect) {
    val now = SystemClock.uptimeMillis()
    if (now - lastLogAt < LOG_INTERVAL_MS) return
    lastLogAt = now
    FileLogger.w(TAG, "$what cached=$cached live=$live")
}
