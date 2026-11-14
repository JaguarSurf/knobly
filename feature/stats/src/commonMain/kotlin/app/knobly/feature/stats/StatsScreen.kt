package app.knobly.feature.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.knobly.core.designsystem.EmptyState
import app.knobly.core.designsystem.KnoblyTheme

/** Placeholder: charts of filtered calls. */
@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = "Statistics",
        message = "Charts of filtered calls per hour, day, week, month and year will appear here.",
        modifier = modifier,
    )
}

@Preview
@Composable
private fun StatsScreenPreview() {
    KnoblyTheme { StatsScreen() }
}
