package app.knobly.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp

/** Full-width pill for the main action of a screen (mint mist in the dark theme, taupe in the light one). */
@Composable
fun KnoblyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = KnoblyTheme.colors
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier =
            modifier
                .defaultMinSize(minHeight = 52.dp)
                .clip(shape)
                .background(c.primaryButton)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .alpha(if (enabled) 1f else 0.5f)
                .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon !=
            null
        ) {
            Icon(icon, contentDescription = null, tint = c.onPrimaryButton, modifier = Modifier.size(18.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = c.onPrimaryButton)
    }
}

/** Outlined pill for secondary actions. */
@Composable
fun KnoblySecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = KnoblyTheme.colors
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier =
            modifier
                .defaultMinSize(minHeight = 52.dp)
                .clip(shape)
                .border(1.dp, if (c.isDark) c.glassBorder else c.divider, shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .alpha(if (enabled) 1f else 0.5f)
                .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = c.text)
    }
}
