package app.knobly.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Translucent "glass" card with a hairline border (and a soft shadow in the light theme). */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    strong: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = KnoblyTheme.colors
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier =
            modifier
                .shadow(
                    elevation = if (c.isDark) 0.dp else 12.dp,
                    shape = shape,
                    ambientColor = c.glassShadow,
                    spotColor = c.glassShadow,
                ).clip(shape)
                .background(if (strong) c.glassStrong else c.glass)
                .border(1.dp, c.glassBorder, shape)
                .padding(contentPadding),
        content = content,
    )
}
