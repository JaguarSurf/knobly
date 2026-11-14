package app.knobly.platform.fake

import app.knobly.domain.port.ScreeningRoleController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fake call screening role for the desktop harness and tests: granting always succeeds. */
class FakeScreeningRoleController(
    roleHeld: Boolean = false,
    override val isRoleAvailable: Boolean = true,
) : ScreeningRoleController {
    private val held = MutableStateFlow(roleHeld)
    override val isRoleHeld: StateFlow<Boolean> = held.asStateFlow()

    override suspend fun requestRole(): Boolean {
        held.value = true
        return true
    }

    /** Simulates the user granting or revoking the role in the system settings. */
    fun setRoleHeld(value: Boolean) {
        held.value = value
    }
}
