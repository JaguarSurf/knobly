package app.knobly.platform.ios.calldirectory

import app.knobly.domain.port.CallLogAccess
import app.knobly.domain.port.CallLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Instant

/** iOS gives apps no access to the call log (docs/platforms.md). */
class IosCallLogAccess : CallLogAccess {
    override val isAvailable: Boolean = false

    override val isGranted: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override suspend fun requestAccess(): Boolean = false

    override fun openAppSettings() = Unit

    override fun incomingCalls(since: Instant): List<CallLogEntry> = emptyList()
}
