package com.cayboard.ui.keyboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.cayboard.viewmodel.ShiftMode

/** Shift state is persistent in every theme, independent of momentary press feedback. */
internal fun shiftKeyColors(
    background: Color,
    foreground: Color,
    accent: Color,
    panel: Color,
    mode: ShiftMode,
    pressed: Boolean,
    legacyStateColor: Color,
    /** 玻璃下功能键更透（见 [glassFunctionKeyScale]）；Shift 开启时的状态色不受影响。 */
    /** 白色玻璃功能键深一档（见 [glassFunctionKeyDepth]）。 */
): KeyboardKeyColors {
    val ordinary = keyStateColor(background, foreground, legacyStateColor,
        pressed = pressed, highlighted = mode != ShiftMode.OFF)
    if (mode == ShiftMode.OFF) return KeyboardKeyColors(ordinary, foreground)

    val base = keyStateColor(background, foreground)
    val locked = mode == ShiftMode.CAPS
    val surfaceBase = base.compositeOver(panel.copy(alpha = 1f))
    fun contrastBetween(a: Color, b: Color): Float {
        val light = maxOf(a.luminance(), b.luminance())
        val dark = minOf(a.luminance(), b.luminance())
        return (light + .05f) / (dark + .05f)
    }
    // Imported/neutral themes can give the accent exactly the same color as the key.
    // Use a contrasting tint in that case; active Shift must not disappear.
    val stateAccent = if (contrastBetween(accent.copy(alpha = 1f), surfaceBase) >= 2f) accent
        else lerp(accent, if (surfaceBase.luminance() < .4f) Color.White else Color.Black, .65f)
    val tint = if (locked) .92f else .70f
    val opacityBoost = (if (locked) 0.35f else 0.18f) + if (pressed) 0.08f else 0f
    // Fully transparent keys stay transparent; the icon still signals the state.
    val fill = lerp(base, stateAccent, tint + if (pressed) .06f else 0f).copy(alpha = if (base.alpha == 0f) 0f
        else base.alpha + (1f - base.alpha) * opacityBoost)
    val surface = fill.compositeOver(panel.copy(alpha = 1f))
    fun contrast(color: Color): Float {
        val a = color.copy(alpha = 1f).luminance()
        val b = surface.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }
    val preferred = if (base.alpha == 0f) accent else foreground
    val icon = if (contrast(preferred) >= 3f) preferred.copy(alpha = 1f)
        else if (contrast(Color.Black) > contrast(Color.White)) Color.Black else Color.White
    return KeyboardKeyColors(fill, icon)
}
