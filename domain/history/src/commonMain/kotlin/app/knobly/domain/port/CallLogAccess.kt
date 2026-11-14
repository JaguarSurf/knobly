package app.knobly.domain.port

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

/**
 * Port: the system call log, read only to learn how the calls that rang ended (answered or missed). Optional: without
 * the permission, those calls stay "rang". Android: `READ_CALL_LOG`. iOS: not available. Desktop and tests: a fake.
 */
interface CallLogAccess {
    val isGranted: StateFlow<Boolean>

    /** Asks for the permission if needed. Returns whether it is granted afterwards. */
    suspend fun requestAccess(): Boolean

    /** Opens the system screen where the user can grant a permission that was denied. */
    fun openAppSettings()

    /** Incoming calls in the call log at or after [since]. Empty without the permission. Blocks. */
    fun incomingCalls(since: Instant): List<CallLogEntry>
}

/** One incoming call of the system call log. */
data class CallLogEntry(
    /** As the system stored it; `null` or empty when hidden. */
    val number: String?,
    /** When the call started ringing. */
    val timestamp: Instant,
    val answered: Boolean,
)
