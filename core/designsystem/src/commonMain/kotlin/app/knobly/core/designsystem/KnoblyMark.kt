package app.knobly.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The Knobly mark, "Fader k": the k whose stem is a fader, with the control handle where the arms meet.
 * Drawn on a 96-unit grid, like docs/design/knobly-icon-dark.svg.
 */
@Composable
fun KnoblyMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tile: Color = KnoblyTheme.colors.surface,
    stroke: Color = KnoblyTheme.colors.text,
    handle: Color = KnoblyTheme.colors.accent,
    contentDescription: String? = null,
) {
    val semantics =
        contentDescription?.let { description -> Modifier.semantics { this.contentDescription = description } }
            ?: Modifier
    Canvas(modifier.size(size).then(semantics)) {
        val u = this.size.minDimension / 96f

        fun p(
            x: Float,
            y: Float,
        ) = Offset(x * u, y * u)
        drawRoundRect(color = tile, cornerRadius = CornerRadius(26 * u))
        val width = 8 * u
        drawLine(stroke, p(34f, 22f), p(34f, 74f), width, StrokeCap.Round)
        drawLine(stroke, p(43f, 50f), p(66f, 26f), width, StrokeCap.Round)
        drawLine(stroke, p(47f, 55f), p(68f, 74f), width, StrokeCap.Round)
        // The handle sits on a ring of the tile color, separating it from the stem and arms.
        drawCircle(tile, radius = 15 * u, center = p(34f, 50f))
        drawCircle(handle, radius = 11 * u, center = p(34f, 50f))
    }
}
