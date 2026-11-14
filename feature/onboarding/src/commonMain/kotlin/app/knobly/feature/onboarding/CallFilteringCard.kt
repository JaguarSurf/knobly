package app.knobly.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.knobly.core.designsystem.GlassCard
import app.knobly.core.designsystem.KnoblyPrimaryButton
import app.knobly.core.designsystem.KnoblyStatus
import app.knobly.core.designsystem.KnoblyTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * Shown while call filtering is off (Knobly is not the call screening app). The system's own phone app stays the
 * default dialer: the call screening role only lets Knobly check incoming calls before they ring.
 * Renders nothing once filtering is enabled.
 */
@Composable
fun CallFilteringCard(
    modifier: Modifier = Modifier,
    viewModel: CallFilteringViewModel = koinViewModel(),
) {
    val roleHeld by viewModel.isRoleHeld.collectAsStateWithLifecycle()
    CallFilteringCardContent(
        isRoleAvailable = viewModel.isRoleAvailable,
        isRoleHeld = roleHeld,
        onEnable = viewModel::requestRole,
        modifier = modifier,
    )
}

@Composable
fun CallFilteringCardContent(
    isRoleAvailable: Boolean,
    isRoleHeld: Boolean,
    onEnable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isRoleHeld) return

    val c = KnoblyTheme.colors
    GlassCard(
        modifier = modifier.fillMaxWidth().testTag(CallFilteringTestTags.CARD),
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KnoblyStatus("Off", active = false)
            Text("Call filtering is off", style = MaterialTheme.typography.titleLarge, color = c.text)
            if (isRoleAvailable) {
                Text(
                    "To apply your filters, allow Knobly to check incoming calls before they ring. " +
                        "Your phone app stays the same, and everything stays on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSoft,
                )
                KnoblyPrimaryButton(
                    text = "Enable call filtering",
                    onClick = onEnable,
                    modifier = Modifier.fillMaxWidth().testTag(CallFilteringTestTags.ENABLE),
                )
            } else {
                Text(
                    "To block calls on iPhone, enable Knobly in " +
                        "Settings > Apps > Phone > Call Blocking & Identification.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSoft,
                )
            }
        }
    }
}

@Preview
@Composable
private fun CallFilteringCardPreview() {
    KnoblyTheme {
        CallFilteringCardContent(isRoleAvailable = true, isRoleHeld = false, onEnable = {})
    }
}
