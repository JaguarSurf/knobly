package app.knobly.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.max

/** Above this many points, only the last one gets a marker (24 hours would be too busy). */
private const val MAX_MARKED_POINTS = 12
private const val AREA_ALPHA = 0.45f
private const val GRID_ALPHA = 0.08f

/**
 * Smooth line chart with a gradient area, point markers and x labels (the Statistics mockup). Labels that would
 * overlap are skipped at a regular step, always keeping the last (current) one. No y axis: the total is shown next
 * to the chart, and [contentDescription] describes it for screen readers.
 */
@Composable
fun KnoblyLineChart(
    values: List<Int>,
    labels: List<String>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    require(values.size == labels.size) { "One label per value" }
    val c = KnoblyTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle =
        MaterialTheme.typography.labelSmall.copy(
            color = c.textSecondary,
            fontSize = 11.sp,
            fontFeatureSettings = "tnum",
        )
    val pointFill = if (c.isDark) c.text else c.surface

    Canvas(
        modifier
            .fillMaxWidth()
            .height(174.dp)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Image
            },
    ) {
        if (values.isEmpty()) return@Canvas
        val padX = 14.dp.toPx()
        val top = 14.dp.toPx()
        val bottom = size.height - 30.dp.toPx()
        val step = if (values.size > 1) (size.width - 2 * padX) / (values.size - 1) else 0f
        val maxValue = max(1, values.max())
        val points =
            values.mapIndexed { index, value ->
                Offset(padX + step * index, bottom - (value.toFloat() / maxValue) * (bottom - top))
            }

        val layouts = labels.map { measurer.measure(it, labelStyle) }
        val labelStride = labelStride(layouts, step, gap = 8.dp.toPx())
        val labeled = points.indices.filter { (points.lastIndex - it) % labelStride == 0 }

        labeled.forEach { index ->
            drawLine(c.text.copy(alpha = GRID_ALPHA), Offset(points[index].x, top / 2), Offset(points[index].x, bottom))
        }

        val line = smoothPath(points)
        val area =
            Path().apply {
                addPath(line)
                lineTo(points.last().x, bottom + 8.dp.toPx())
                lineTo(points.first().x, bottom + 8.dp.toPx())
                close()
            }
        drawPath(
            area,
            Brush.verticalGradient(
                listOf(c.toggleOn.copy(alpha = AREA_ALPHA), c.toggleOn.copy(alpha = 0f)),
                startY = top,
                endY = bottom + 8.dp.toPx(),
            ),
        )
        drawPath(line, c.accent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        val marked = if (points.size <= MAX_MARKED_POINTS) points else listOf(points.last())
        marked.forEach { point ->
            drawCircle(pointFill, radius = 5.dp.toPx(), center = point)
            drawCircle(c.toggleOn, radius = 5.dp.toPx(), center = point, style = Stroke(width = 2.dp.toPx()))
        }

        labeled.forEach { index -> drawLabel(layouts[index], points[index].x, size.height) }
    }
}

/** The smallest step (1, 2, 3…) at which labels, centered on their points, do not overlap. */
private fun labelStride(
    layouts: List<TextLayoutResult>,
    step: Float,
    gap: Float,
): Int {
    val widest = layouts.maxOf { it.size.width }.toFloat()
    return if (step <= 0f) 1 else max(1, ceil((widest + gap) / step).toInt())
}

/** Cubic curve through every point, with horizontal tangents (no overshoot below zero). */
private fun smoothPath(points: List<Offset>): Path =
    Path().apply {
        moveTo(points.first().x, points.first().y)
        points.zipWithNext { from, to ->
            val midX = (from.x + to.x) / 2
            cubicTo(midX, from.y, midX, to.y, to.x, to.y)
        }
    }

private fun DrawScope.drawLabel(
    layout: TextLayoutResult,
    centerX: Float,
    height: Float,
) {
    val width = layout.size.width.toFloat()
    val x = (centerX - width / 2).coerceIn(0f, size.width - width)
    drawText(layout, topLeft = Offset(x, height - layout.size.height))
}
