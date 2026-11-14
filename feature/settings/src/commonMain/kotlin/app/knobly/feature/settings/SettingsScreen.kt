package app.knobly.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.knobly.core.designsystem.EmptyState
import app.knobly.core.designsystem.KnoblyTheme
import app.knobly.feature.settings.resources.Res
import app.knobly.feature.settings.resources.settings_empty_message
import app.knobly.feature.settings.resources.settings_title
import org.jetbrains.compose.resources.stringResource

/** Placeholder: general settings, backup and import/export. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(Res.string.settings_title),
        message = stringResource(Res.string.settings_empty_message),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    KnoblyTheme { SettingsScreen() }
}
