package app.knobly.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A pill-shaped choice (module selector, history filter). Its text never breaks: put chips in a [KnoblyChipRow],
 * which keeps them on one line on narrow screens and only moves one to the next line when even that cannot work.
 */
@Composable
fun KnoblyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = KnoblyTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val (background, border, content) =
        when {
            selected && c.isDark -> {
                Triple(c.glassStrong, c.accent, c.text)
            }

            selected -> {
                Triple(c.selected, c.selected, c.onSelected)
            }

            else -> {
                Triple(
                    c.glass,
                    if (c.isDark) c.glassBorder else c.divider,
                    if (enabled) c.textSoft else c.textSecondary,
                )
            }
        }
    Box(
        modifier =
            modifier
                .defaultMinSize(minHeight = 40.dp)
                .clip(shape)
                .background(background)
                .border(1.dp, border, shape)
                .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = onClick)
                .alpha(if (enabled) 1f else 0.6f)
                .padding(horizontal = LocalChipHorizontalPadding.current),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1, softWrap = false)
    }
}

/** Equal-width segmented control (e.g. Hour / Day / Week / Month / Year). */
@Composable
fun KnoblySegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = KnoblyTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier =
            modifier
                .clip(shape)
                .background(c.glass)
                .border(1.dp, c.glassBorder, shape)
                .padding(4.dp)
                .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) c.selected else c.glass.copy(alpha = 0f))
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) c.onSelected else c.textSoft,
                )
            }
        }
    }
}

/** 60 dp round "mode" button with a label below; the selected one gets an accent ring (and glow when dark). */
@Composable
fun KnoblyModeButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = KnoblyTheme.colors
    Column(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .then(if (selected) Modifier.knoblyGlow() else Modifier)
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(if (selected) c.glassStrong else c.glass)
                    .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.glassBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) c.accent else c.textSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            label,
            style = if (selected) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
            color = if (selected) c.text else c.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
