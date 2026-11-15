package app.knobly.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A highlighted card. In the dark theme it is a [GlassCard]; in the light theme it is a block of softened Noturno
 * ([KnoblyTheme.blockColors]) and its content is drawn with those colors, so every component inside it reads
 * correctly on the dark block.
 */
@Composable
fun KnoblyBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val outer = KnoblyTheme.colors
    if (outer.isDark) {
        GlassCard(modifier, cornerRadius, contentPadding, content = content)
        return
    }
    val block = KnoblyTheme.blockColors
    val shape = RoundedCornerShape(cornerRadius)
    ProvideBlockColors {
        Column(
            modifier =
                modifier
                    .shadow(16.dp, shape, ambientColor = outer.glassShadow, spotColor = outer.glassShadow)
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(block.backgroundTop, block.backgroundBottom)))
                    .border(1.dp, block.glassBorder, shape)
                    .padding(contentPadding),
            content = content,
        )
    }
}

/** Draws [content] with the theme's block colors ([KnoblyTheme.blockColors]). */
@Composable
internal fun ProvideBlockColors(content: @Composable () -> Unit) {
    val block = KnoblyTheme.blockColors
    CompositionLocalProvider(
        LocalKnoblyColors provides block,
        LocalContentColor provides block.text,
        content = content,
    )
}
