package app.knobly.core.designsystem

import androidx.compose.ui.graphics.vector.ImageVector

/** One destination of the [KnoblyNavBar]. */
data class KnoblyNavItem(
    val label: String,
    val icon: ImageVector,
    val testTag: String,
)
