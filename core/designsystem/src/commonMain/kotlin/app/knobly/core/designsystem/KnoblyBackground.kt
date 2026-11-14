package app.knobly.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Screen background: the theme's vertical gradient plus a soft glow near the top. */
@Composable
fun KnoblyBackground(
    modifier: Modifier = Modifier,
    glow: GlowPosition = GlowPosition.TopEnd,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = KnoblyTheme.colors
    Box(
        modifier =
            modifier
                .background(Brush.verticalGradient(listOf(c.backgroundTop, c.backgroundMiddle, c.backgroundBottom)))
                .drawBehind {
                    val center = Offset(size.width * glow.x, size.height * glow.y)
                    val radius = size.minDimension * 0.85f
                    drawCircle(
                        brush = Brush.radialGradient(listOf(c.backgroundGlow, Color.Transparent), center, radius),
                        radius = radius,
                        center = center,
                    )
                },
        content = content,
    )
}
