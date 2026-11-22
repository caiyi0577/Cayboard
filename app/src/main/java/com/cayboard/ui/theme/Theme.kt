package com.cayboard.ui.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** One fixed visual language with system light/dark appearances. */
@Composable
fun CayboardTheme(darkTheme: Boolean = true, themeId: String? = null, content: @Composable () -> Unit) {
    val s = FixedKeyboardStyle.scheme
    val surface = if (darkTheme) s.surfaceDark else s.surfaceLight
    val ink = if (darkTheme) s.candidateTextColorDark else s.candidateTextColorLight
    val selection = SelectionStyle.colors(darkTheme)
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    val colors = base.copy(
        primary = if (darkTheme) s.primaryDark else s.primaryLight,
        primaryContainer = if (darkTheme) s.primaryContainerDark else s.primaryContainerLight,
        onPrimary = if (darkTheme) androidx.compose.ui.graphics.Color(0xFF163847) else androidx.compose.ui.graphics.Color.White,
        onPrimaryContainer = selection.quietContent,
        secondary = if (darkTheme) s.accentDark else s.accentLight,
        onSecondary = if (darkTheme) androidx.compose.ui.graphics.Color(0xFF163847) else androidx.compose.ui.graphics.Color.White,
        secondaryContainer = selection.quietFill, onSecondaryContainer = selection.quietContent,
        tertiary = if (darkTheme) s.accentDark else s.accentLight,
        onTertiary = if (darkTheme) androidx.compose.ui.graphics.Color(0xFF163847) else androidx.compose.ui.graphics.Color.White,
        tertiaryContainer = selection.quietFill, onTertiaryContainer = selection.quietContent,
        outline = if (darkTheme) androidx.compose.ui.graphics.Color(0xFF87959E) else androidx.compose.ui.graphics.Color(0xFF7B8993),
        outlineVariant = if (darkTheme) androidx.compose.ui.graphics.Color(0xFF46535B) else androidx.compose.ui.graphics.Color(0xFFC5CFD6),
        surfaceTint = if (darkTheme) s.accentDark else s.accentLight,
        surfaceVariant = if (darkTheme) androidx.compose.ui.graphics.Color(0xFF2C3236) else androidx.compose.ui.graphics.Color(0xFFE5E9ED),
        surfaceContainerHigh = if (darkTheme) androidx.compose.ui.graphics.Color(0xFF30383C) else androidx.compose.ui.graphics.Color(0xFFE6EAEC),
        surfaceContainerHighest = if (darkTheme) androidx.compose.ui.graphics.Color(0xFF3A4449) else androidx.compose.ui.graphics.Color(0xFFDCE2E6),
        background = surface, surface = surface,
        onBackground = ink, onSurface = ink, onSurfaceVariant = ink,
        surfaceContainerLow = if (darkTheme) androidx.compose.ui.graphics.Color(0xFF2A3033) else androidx.compose.ui.graphics.Color(0xFFECEEEF),
        surfaceContainer = if (darkTheme) androidx.compose.ui.graphics.Color(0xFF30383C) else androidx.compose.ui.graphics.Color(0xFFE6EAEC),
    )
    CompositionLocalProvider(LocalTextSelectionColors provides TextSelectionColors(
        handleColor = if (darkTheme) s.primaryDark else s.primaryLight, backgroundColor = selection.quietFill),
        LocalKeyboardPalette provides resolveKeyboardPalette(FixedKeyboardStyle.ID, darkTheme),
        LocalDensity provides Density(LocalDensity.current.density, 1f)) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
