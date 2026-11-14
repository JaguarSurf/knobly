package app.knobly.domain.port

import kotlinx.coroutines.flow.StateFlow

/**
 * Port: whether Knobly is the system's call screening app, and how to become one.
 *
 * Android: `RoleManager.ROLE_CALL_SCREENING`. iOS: there is no such role (the user enables the Call Directory
 * extension in Settings). Desktop harness and tests: a fake. See docs/architecture.md, "Platform ports".
 */
interface ScreeningRoleController {
    /** `false` when the platform has no call screening role at all (iOS, desktop). */
    val isRoleAvailable: Boolean

    val isRoleHeld: StateFlow<Boolean>

    /** Asks the system/user for the role. Returns whether the role is held afterwards. */
    suspend fun requestRole(): Boolean
}
