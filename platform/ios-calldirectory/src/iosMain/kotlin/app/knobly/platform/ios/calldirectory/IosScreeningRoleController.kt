package app.knobly.platform.ios.calldirectory

import app.knobly.domain.port.ScreeningRoleController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS has no call screening role: blocking happens through the Call Directory Extension, which the user enables
 * in Settings. Exporting rules to the extension (via App Group) will live in this module.
 * See docs/platforms.md.
 */
class IosScreeningRoleController : ScreeningRoleController {
    override val isRoleAvailable: Boolean = false
    override val isRoleHeld: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override suspend fun requestRole(): Boolean = false
}
