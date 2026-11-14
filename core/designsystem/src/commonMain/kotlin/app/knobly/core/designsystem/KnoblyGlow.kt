package app.knobly.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Soft accent glow behind selected elements (dark theme only; the light theme has no glow). */
@Composable
fun Modifier.knoblyGlow(): Modifier {
    val glow = KnoblyTheme.colors.glow
    if (glow == Color.Transparent) return this
    return drawBehind {
        val radius = size.maxDimension * 0.9f
        drawCircle(brush = Brush.radialGradient(listOf(glow, Color.Transparent), center, radius), radius = radius)
    }
}
