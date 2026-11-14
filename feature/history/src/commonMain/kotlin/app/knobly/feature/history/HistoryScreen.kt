package app.knobly.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.knobly.core.designsystem.EmptyState
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.feature.history.resources.Res
import app.knobly.feature.history.resources.history_empty_message
import app.knobly.feature.history.resources.history_title
import org.jetbrains.compose.resources.stringResource

/** Placeholder: the list of filtered calls. */
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(Res.string.history_title),
        message = stringResource(Res.string.history_empty_message),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun HistoryScreenPreview() {
    KnoblyTheme { HistoryScreen() }
}
