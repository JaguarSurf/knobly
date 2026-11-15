package app.knobly.core.designsystem

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/** Horizontal padding of a [KnoblyChip] on its own. */
internal val ChipPadding = 14.dp

/** The least horizontal padding a [KnoblyChipRow] gives its chips before moving one to the next line. */
internal val ChipMinPadding = 6.dp

private val ChipSpacing = 8.dp
private val ChipMinSpacing = 6.dp

/**
 * The padding a [KnoblyChip] lays its text out with. A [KnoblyChipRow] sets it to [ChipMinPadding] and widens the
 * chips itself, so it can choose how much room each one gets.
 */
internal val LocalChipHorizontalPadding = staticCompositionLocalOf<Dp> { ChipPadding }

/**
 * A row of [KnoblyChip]s that keeps them on one line on narrow phones without ever breaking their text:
 * - when they fit with their normal padding, they keep it (start-aligned, 8 dp apart);
 * - otherwise their padding shrinks (down to [ChipMinPadding], 6 dp apart) and they share the whole width;
 * - only when even that does not fit (very small screens, large fonts) does a chip move to the next line.
 */
@Composable
fun KnoblyChipRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalChipHorizontalPadding provides ChipMinPadding) {
        Layout(content, modifier.selectableGroup()) { measurables, constraints ->
            val slots =
                arrangeChips(
                    minWidths = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) },
                    maxWidth = constraints.maxWidth,
                    spacing = ChipSpacing.roundToPx(),
                    minSpacing = ChipMinSpacing.roundToPx(),
                    extraPadding = ((ChipPadding - ChipMinPadding) * 2).roundToPx(),
                )
            val placeables =
                measurables.zip(slots) { measurable, slot ->
                    measurable.measure(Constraints(slot.width, slot.width, 0, constraints.maxHeight))
                }
            val lineCount = (slots.maxOfOrNull { it.line } ?: -1) + 1
            val lineHeights = IntArray(lineCount)
            slots.forEachIndexed {
                i,
                slot,
                ->
                lineHeights[slot.line] = max(lineHeights[slot.line], placeables[i].height)
            }
            val lineSpacing = ChipSpacing.roundToPx()
            val lineTops = IntArray(lineCount)
            for (line in 1 until lineCount) lineTops[line] = lineTops[line - 1] + lineHeights[line - 1] + lineSpacing
            val contentWidth = slots.maxOfOrNull { it.x + it.width } ?: 0
            val contentHeight = if (lineCount == 0) 0 else lineTops.last() + lineHeights.last()
            layout(
                contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth),
                contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight),
            ) {
                placeables.forEachIndexed { i, placeable ->
                    val slot = slots[i]
                    val centered = (lineHeights[slot.line] - placeable.height) / 2
                    placeable.place(slot.x, lineTops[slot.line] + centered)
                }
            }
        }
    }
}

/** Where a chip goes: its line (from 0), its x on that line and its width, in pixels. */
internal data class ChipSlot(
    val line: Int,
    val x: Int,
    val width: Int,
)

/**
 * The layout of a [KnoblyChipRow], in pixels. [minWidths] are the chips' widths with [ChipMinPadding];
 * [extraPadding] is what their normal padding adds to each one.
 */
internal fun arrangeChips(
    minWidths: List<Int>,
    maxWidth: Int,
    spacing: Int,
    minSpacing: Int,
    extraPadding: Int,
): List<ChipSlot> {
    val count = minWidths.size
    val natural = minWidths.map { it + extraPadding }
    val compact = minWidths.sum() + minSpacing * (count - 1)
    return when {
        natural.sum().toLong() + spacing.toLong() * (count - 1) <= maxWidth -> oneLine(natural, spacing)

        compact <= maxWidth -> oneLine(shareLeftover(minWidths, maxWidth - compact), minSpacing)

        // Too narrow even with the least padding: wrap, keeping each chip whole.
        else -> wrap(natural, maxWidth, spacing)
    }
}

/** Shares [leftover] pixels equally between the chips, so the row spans the whole width. */
private fun shareLeftover(
    widths: List<Int>,
    leftover: Int,
): List<Int> =
    widths.mapIndexed { i, width ->
        width + leftover / widths.size +
            if (i <
                leftover % widths.size
            ) {
                1
            } else {
                0
            }
    }

private fun wrap(
    widths: List<Int>,
    maxWidth: Int,
    spacing: Int,
): List<ChipSlot> {
    var line = 0
    var x = 0
    return widths.map {
        val width = min(it, maxWidth)
        if (x > 0 && x + width > maxWidth) {
            line++
            x = 0
        }
        ChipSlot(line, x, width).also { x += width + spacing }
    }
}

private fun oneLine(
    widths: List<Int>,
    spacing: Int,
): List<ChipSlot> {
    var x = 0
    return widths.map { width -> ChipSlot(0, x, width).also { x += width + spacing } }
}
