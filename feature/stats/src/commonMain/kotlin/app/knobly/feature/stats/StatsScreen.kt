package app.knobly.feature.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.knobly.core.designsystem.EmptyState
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.feature.stats.resources.Res
import app.knobly.feature.stats.resources.stats_empty_message
import app.knobly.feature.stats.resources.stats_title
import org.jetbrains.compose.resources.stringResource

/** Placeholder: charts of filtered calls. */
@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(Res.string.stats_title),
        message = stringResource(Res.string.stats_empty_message),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun StatsScreenPreview() {
    KnoblyTheme { StatsScreen() }
}
