package app.knobly.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val FADERS_ANIMATION_MS = 400

/**
 * Three mixer faders on a tile — the brand's "control" illustration. Faders rise to their [levels] (0..1) while
 * [active] and drop to the bottom when inactive.
 */
@Composable
fun KnoblyFaders(
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 104.dp,
    levels: List<Float> = listOf(0.55f, 0.88f, 0.71f),
) {
    val c = KnoblyTheme.colors
    val animated = levels.map { level -> animateFloatAsState(if (active) level else 0f, tween(FADERS_ANIMATION_MS)) }
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 96f
        drawRoundRect(color = if (c.isDark) c.surface else c.glassStrong, cornerRadius = CornerRadius(24 * u))
        val top = 24 * u
        val bottom = 72 * u
        val width = 5 * u
        listOf(30f, 48f, 66f).forEachIndexed { index, x ->
            val cx = x * u
            drawLine(c.text.copy(alpha = 0.14f), Offset(cx, top), Offset(cx, bottom), width, StrokeCap.Round)
            val level by animated[index]
            val y = bottom - (bottom - top) * level
            drawLine(c.accent, Offset(cx, bottom), Offset(cx, y), width, StrokeCap.Round)
            drawCircle(if (c.isDark) c.text else c.onAccent, radius = 7.5f * u, center = Offset(cx, y))
            if (!c.isDark) drawCircle(c.accent, radius = 7.5f * u, center = Offset(cx, y), style = Stroke(3 * u))
        }
    }
}
