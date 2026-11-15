package app.knobly.platform.fake

import app.knobly.domain.port.CallLogAccess
import app.knobly.domain.port.CallLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Instant

/**
 * Fake call log for the desktop harness and tests: [log] what happened to a call (the harness does it after a call
 * rings, or for a call Knobly did not screen). [grantOnRequest] decides what happens when the app asks for the
 * permission.
 */
class FakeCallLogAccess(
    granted: Boolean = true,
    var grantOnRequest: Boolean = true,
) : CallLogAccess {
    private val grantedState = MutableStateFlow(granted)
    private val entries = mutableListOf<CallLogEntry>()
    override val isGranted: StateFlow<Boolean> = grantedState.asStateFlow()
    var settingsOpened = 0
        private set

    override suspend fun requestAccess(): Boolean {
        if (grantOnRequest) grantedState.value = true
        return grantedState.value
    }

    override fun openAppSettings() {
        settingsOpened++
    }

    override fun incomingCalls(since: Instant): List<CallLogEntry> =
        if (grantedState.value) entries.filter { it.timestamp >= since }.sortedBy { it.timestamp } else emptyList()

    fun log(entry: CallLogEntry) {
        entries += entry
    }

    /** Simulates the user granting or revoking the permission in the system settings. */
    fun setGranted(value: Boolean) {
        grantedState.value = value
    }
}
