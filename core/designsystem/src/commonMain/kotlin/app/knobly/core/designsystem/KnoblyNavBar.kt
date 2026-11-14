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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Floating glass navigation bar with icon-only tabs (labels are read by screen readers). */
@Composable
fun KnoblyNavBar(
    items: List<KnoblyNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = KnoblyTheme.colors
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(if (c.isDark) 0.dp else 12.dp, shape, ambientColor = c.glassShadow, spotColor = c.glassShadow)
                .clip(shape)
                .background(c.glass)
                .border(1.dp, c.glassBorder, shape)
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
                        c.isDark -> Modifier.knoblyGlow().clip(RoundedCornerShape(18.dp)).background(c.glassStrong)
                        else -> Modifier.clip(RoundedCornerShape(18.dp)).background(c.accent)
                    }
                Box(Modifier.size(width = 52.dp, height = 44.dp).then(indicator), contentAlignment = Alignment.Center) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint =
                            when {
                                !selected -> c.textSecondary
                                c.isDark -> c.accent
                                else -> c.onAccent
                            },
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
