package app.knobly.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** Status dot with a short label, e.g. "Protegido" / "Desligado". */
@Composable
fun KnoblyStatus(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    framed: Boolean = false,
) {
    val c = KnoblyTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier =
            modifier.then(
                if (framed) {
                    Modifier
                        .clip(
                            shape,
                        ).background(
                            c.glass,
                        ).border(1.dp, c.glassBorder, shape)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                } else {
                    Modifier
                },
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (active) c.statusActive else c.statusInactive))
        Text(text, style = MaterialTheme.typography.bodySmall, color = if (framed) c.text else c.textSecondary)
    }
}
