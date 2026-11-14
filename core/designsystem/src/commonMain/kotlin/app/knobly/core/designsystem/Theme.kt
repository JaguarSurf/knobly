package app.knobly.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Teal brand palette with neutral, slightly teal-tinted surfaces. Amber is the accent (tertiary).
private val LightColors =
    lightColorScheme(
        primary = Color(0xFF00796B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB2DFDB),
        onPrimaryContainer = Color(0xFF00201C),
        secondary = Color(0xFF4A635F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCE8E2),
        onSecondaryContainer = Color(0xFF05201C),
        tertiary = Color(0xFF8A5100),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDCBE),
        onTertiaryContainer = Color(0xFF2C1600),
        background = Color(0xFFF5FBF9),
        onBackground = Color(0xFF171D1C),
        surface = Color(0xFFF5FBF9),
        onSurface = Color(0xFF171D1C),
        surfaceVariant = Color(0xFFDAE5E1),
        onSurfaceVariant = Color(0xFF3F4947),
        surfaceContainer = Color(0xFFE9EFED),
        outline = Color(0xFF6F7977),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF80CBC4),
        onPrimary = Color(0xFF003733),
        primaryContainer = Color(0xFF005048),
        onPrimaryContainer = Color(0xFFB2DFDB),
        secondary = Color(0xFFB1CCC6),
        onSecondary = Color(0xFF1C3531),
        secondaryContainer = Color(0xFF334B47),
        onSecondaryContainer = Color(0xFFCCE8E2),
        tertiary = Color(0xFFFFB86E),
        onTertiary = Color(0xFF492900),
        tertiaryContainer = Color(0xFF693C00),
        onTertiaryContainer = Color(0xFFFFDCBE),
        background = Color(0xFF0E1513),
        onBackground = Color(0xFFDEE4E1),
        surface = Color(0xFF0E1513),
        onSurface = Color(0xFFDEE4E1),
        surfaceVariant = Color(0xFF3F4947),
        onSurfaceVariant = Color(0xFFBEC9C6),
        surfaceContainer = Color(0xFF1A2120),
        outline = Color(0xFF899390),
    )

/** Knobly's Material 3 theme, shared by every platform. */
@Composable
fun KnoblyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
