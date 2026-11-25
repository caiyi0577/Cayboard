package com.cayboard.ui.settings

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Velocity

internal data object SettingsOverscrollFactory : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = SettingsOverscrollEffect()
}

private class SettingsOverscrollEffect : OverscrollEffect {
    private var pull by mutableFloatStateOf(0f)
    override val isInProgress get() = pull != 0f
    override val node: Modifier.Node = object : Modifier.Node(), DrawModifierNode {
        override fun ContentDrawScope.draw() {
            val limit = 56f * density
            val displacement = pull * 0.42f
            translate(top = displacement / (1f + kotlin.math.abs(displacement) / limit)) {
                this@draw.drawContent()
            }
        }
    }

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        if (source != NestedScrollSource.UserInput) return performScroll(delta)
        val released = if (delta.y * pull < 0f) {
            delta.y.coerceIn(minOf(-pull, 0f), maxOf(-pull, 0f))
        } else 0f
        pull += released
        val remaining = delta - Offset(0f, released)
        val consumed = performScroll(remaining)
        val edge = remaining.y - consumed.y
        pull += edge
        return Offset(consumed.x, delta.y)
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        if (!isInProgress) {
            performFling(velocity)
            return
        }
        performFling(Velocity(velocity.x, 0f))
        animate(pull, 0f, animationSpec = spring(dampingRatio = 0.82f, stiffness = 430f)) { value, _ ->
            pull = value
        }
        pull = 0f
    }
}

