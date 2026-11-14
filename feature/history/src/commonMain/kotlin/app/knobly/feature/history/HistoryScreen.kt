package app.knobly.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.knobly.core.designsystem.EmptyState
import app.knobly.core.designsystem.KnoblyTheme

/** Placeholder: the list of filtered calls. */
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = "History",
        message = "Calls filtered by Knobly will appear here.",
        modifier = modifier,
    )
}

@Preview
@Composable
private fun HistoryScreenPreview() {
    KnoblyTheme { HistoryScreen() }
}
