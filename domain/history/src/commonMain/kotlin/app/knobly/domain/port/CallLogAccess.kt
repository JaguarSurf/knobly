package app.knobly.domain.port

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

/**
 * Port: the system call log, only read, to bring in the incoming calls Knobly did not screen and to learn how the
 * calls that rang ended (answered or missed). Optional: without the permission, History shows only the calls Knobly
 * screened and those that rang stay "rang". Android: `READ_CALL_LOG`. iOS: not available. Desktop and tests: a fake.
 */
interface CallLogAccess {
    /** Whether the platform has a call log apps can read (not on iOS). */
    val isAvailable: Boolean get() = true

    val isGranted: StateFlow<Boolean>

    /** Asks for the permission if needed. Returns whether it is granted afterwards. */
    suspend fun requestAccess(): Boolean

    /** Opens the system screen where the user can grant a permission that was denied. */
    fun openAppSettings()

    /** Incoming calls in the call log at or after [since], oldest first. Empty without the permission. Blocks. */
    fun incomingCalls(since: Instant): List<CallLogEntry>
}

/** One incoming call of the system call log. */
data class CallLogEntry(
    /** As the system stored it; `null` or empty when hidden. */
    val number: String?,
    /** When the call started ringing. */
    val timestamp: Instant,
    val type: CallLogType,
) {
    /**
     * Identifies the entry across reads: start time and number. Unlike the call log's row id, it survives a restore of
     * the call log.
     */
    val key: String get() = "${timestamp.toEpochMilliseconds()}:${number.orEmpty()}"
}

/** How an incoming call of the call log ended. */
enum class CallLogType {
    /** Answered, here or on another device. */
    ANSWERED,

    /** Missed, declined by the user or sent to voicemail. */
    MISSED,

    /** Blocked by the system or by an app, Knobly included (its rejections are linked, not brought in again). */
    BLOCKED,
}
