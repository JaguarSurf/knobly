package app.knobly.feature.calls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.knobly.core.designsystem.EmptyState
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.feature.calls.resources.Res
import app.knobly.feature.calls.resources.filters_empty_message
import app.knobly.feature.calls.resources.filters_title
import org.jetbrains.compose.resources.stringResource

/** Placeholder: list, create and reorder call filter rules. */
@Composable
fun FiltersScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(Res.string.filters_title),
        message = stringResource(Res.string.filters_empty_message),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun FiltersScreenPreview() {
    KnoblyTheme { FiltersScreen() }
}
