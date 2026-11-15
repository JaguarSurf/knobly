package app.knobly.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Floating navigation bar with icon-only tabs (labels are read by screen readers): glass in the dark theme, a block
 * of softened Noturno ([KnoblyTheme.blockColors]) in the light theme.
 */
@Composable
fun KnoblyNavBar(
    items: List<KnoblyNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outer = KnoblyTheme.colors
    val c = KnoblyTheme.blockColors
    val shape = RoundedCornerShape(26.dp)
    ProvideBlockColors {
        NavBarRow(items, selectedIndex, onSelect, lightTheme = !outer.isDark, outer.glassShadow, c, shape, modifier)
    }
}

@Composable
private fun NavBarRow(
    items: List<KnoblyNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    lightTheme: Boolean,
    shadowColor: Color,
    c: KnoblyColors,
    shape: Shape,
    modifier: Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(if (lightTheme) 16.dp else 0.dp, shape, ambientColor = shadowColor, spotColor = shadowColor)
                .clip(shape)
                .then(
                    if (lightTheme) {
                        Modifier.background(Brush.verticalGradient(listOf(c.backgroundTop, c.backgroundBottom)))
                    } else {
                        Modifier.background(c.glass)
                    },
                ).border(1.dp, c.glassBorder, shape)
                .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(68.dp)
                        .selectable(
                            selected = selected,
                            interactionSource = null,
                            // Circular ripple around the icon instead of highlighting the whole cell.
                            indication = ripple(bounded = false, radius = 28.dp),
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ).semantics { contentDescription = item.label }
                        .testTag(item.testTag),
                contentAlignment = Alignment.Center,
            ) {
                val indicator =
                    when {
                        !selected -> Modifier
                        lightTheme -> Modifier.knoblyGlow().clip(RoundedCornerShape(18.dp)).background(c.selected)
                        else -> Modifier.knoblyGlow().clip(RoundedCornerShape(18.dp)).background(c.glassStrong)
                    }
                Box(Modifier.size(width = 52.dp, height = 44.dp).then(indicator), contentAlignment = Alignment.Center) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint =
                            when {
                                !selected -> c.textSecondary
                                lightTheme -> c.onSelected
                                else -> c.accent
                            },
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
