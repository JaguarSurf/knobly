package app.knobly.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp

/**
 * A full-width row with an optional round icon, a title, an optional subtitle and a [KnoblyToggle].
 * The whole row is the touch target and is announced as a switch.
 */
@Composable
fun KnoblyToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = KnoblyTheme.colors
    Row(
        modifier =
            modifier
                .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                .alpha(if (enabled) 1f else 0.45f)
                .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            KnoblyRoundIcon(icon, highlighted = checked)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
        KnoblyToggle(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** 40 dp round icon badge; highlighted badges use the accent color (and glow in the dark theme). */
@Composable
fun KnoblyRoundIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val c = KnoblyTheme.colors
    Box(
        modifier =
            modifier
                .then(if (highlighted) Modifier.knoblyGlow() else Modifier)
                .size(40.dp)
                .clip(CircleShape)
                .background(if (highlighted) c.glassStrong else c.glass),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (highlighted) c.accent else c.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
