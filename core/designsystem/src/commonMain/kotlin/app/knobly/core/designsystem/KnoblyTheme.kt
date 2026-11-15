package app.knobly.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Knobly's theme, shared by every platform: dark "Noturno" or light "Noturno Partido", following the system setting.
 * See docs/design/visual-identity.md.
 */
@Composable
fun KnoblyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkKnoblyColors else LightKnoblyColors
    val blockColors = if (darkTheme) DarkKnoblyColors else LightBlockKnoblyColors
    CompositionLocalProvider(LocalKnoblyColors provides colors, LocalKnoblyBlockColors provides blockColors) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = knoblyTypography(),
        ) {
            // Screens draw on KnoblyBackground (not a Material Surface), so set the default text/icon color here.
            CompositionLocalProvider(LocalContentColor provides colors.text, content = content)
        }
    }
}

object KnoblyTheme {
    /** The current Knobly color tokens. */
    val colors: KnoblyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKnoblyColors.current

    /** The colors of the theme's dark blocks ([KnoblyBlock], [KnoblyNavBar]); [colors] itself in the dark theme. */
    val blockColors: KnoblyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKnoblyBlockColors.current
}

private fun KnoblyColors.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = glassStrong,
        onPrimaryContainer = text,
        secondary = toggleOn,
        onSecondary = thumbOn,
        background = backgroundMiddle,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = glass,
        onSurfaceVariant = textSecondary,
        surfaceContainer = surface,
        outline = textSecondary,
        outlineVariant = divider,
        error = alert,
        scrim = Color.Black.copy(alpha = 0.5f),
    )
}
