package app.knobly.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.knobly.core.designsystem.EmptyState
import app.knobly.core.designsystem.KnoblyTheme

/** Placeholder: general settings, backup and import/export. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = "Settings",
        message = "General settings, backup and rule import/export will be available here.",
        modifier = modifier,
    )
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    KnoblyTheme { SettingsScreen() }
}
