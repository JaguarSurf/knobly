package app.knobly.feature.calls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.knobly.core.designsystem.EmptyState
import app.knobly.core.designsystem.KnoblyTheme

/** Placeholder: list, create and reorder call filter rules. */
@Composable
fun FiltersScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = "Filters",
        message = "Your call filter rules will be listed here.",
        modifier = modifier,
    )
}

@Preview
@Composable
private fun FiltersScreenPreview() {
    KnoblyTheme { FiltersScreen() }
}
