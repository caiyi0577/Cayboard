package com.cayboard.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable internal data class MaterialPalette(val surface: Color, val accent: Color)
internal val LocalMaterialPalette = staticCompositionLocalOf<MaterialPalette?> { null }
