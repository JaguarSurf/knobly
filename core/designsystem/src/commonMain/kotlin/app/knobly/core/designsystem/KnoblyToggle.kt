package app.knobly.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private const val TOGGLE_ANIMATION_MS = 220

/**
 * Knobly's on/off switch (52×30 dp, 48 dp touch target). With [onCheckedChange] `null` it is display-only, for
 * rows that handle the click themselves (see [KnoblyToggleRow]).
 */
@Composable
fun KnoblyToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = KnoblyTheme.colors
    val thumbOffset by animateDpAsState(if (checked) 25.dp else 3.dp, tween(TOGGLE_ANIMATION_MS))
    val track by animateColorAsState(if (checked) c.toggleOn else c.toggleOff, tween(TOGGLE_ANIMATION_MS))
    val thumb by animateColorAsState(if (checked) c.thumbOn else c.thumbOff, tween(TOGGLE_ANIMATION_MS))
    val interaction =
        if (onCheckedChange != null) {
            Modifier
                .minimumInteractiveComponentSize()
                .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
        } else {
            Modifier
        }

    Box(modifier.then(interaction).alpha(if (enabled) 1f else 0.4f)) {
        Box(
            Modifier
                .size(width = 52.dp, height = 30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(track),
        ) {
            Box(
                Modifier
                    .offset(x = thumbOffset, y = 3.dp)
                    .size(24.dp)
                    .shadow(if (c.isDark) 0.dp else 2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(thumb),
            )
        }
    }
}
