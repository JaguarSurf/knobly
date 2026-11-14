package app.knobly.domain.history

import app.knobly.core.phone.PhoneNumberMatching
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.port.CallLogAccess
import app.knobly.domain.port.CallLogEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/**
 * Finds the calls that rang in the system call log and records whether they were answered or missed. Runs when the
 * app shows calls and after each screened call; does nothing without the call log permission. Calls that are not in
 * the call log (yet) stay "rang" and are tried again later, for [LOOKBACK].
 */
class CallOutcomeResolver(
    private val history: CallHistoryRepository,
    private val callLog: CallLogAccess,
    private val normalizer: PhoneNumberNormalizer,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()

    /** Whether Knobly may read the call log (History and Statistics offer to ask when it may not). */
    val callLogGranted: StateFlow<Boolean> = callLog.isGranted

    /** Asks for the call log permission and, if granted, resolves the pending calls right away. */
    suspend fun requestCallLogAccess(): Boolean =
        callLog.requestAccess().also { granted ->
            if (granted) resolvePending()
        }

    fun openCallLogSettings() = callLog.openAppSettings()

    suspend fun resolvePending() {
        if (!callLog.isGranted.value) return
        mutex.withLock {
            val pending = history.pendingOutcomes(clock.now() - LOOKBACK)
            if (pending.isEmpty()) return
            val entries = withContext(dispatcher) { callLog.incomingCalls(pending.first().timestamp - MATCH_WINDOW) }
            val unused = entries.toMutableList()
            pending.forEach { call ->
                val entry = unused.closestMatch(call) ?: return@forEach
                unused.remove(entry)
                history.setOutcome(call.id, if (entry.answered) CallOutcome.ANSWERED else CallOutcome.MISSED)
            }
        }
    }

    /** The call log entry of [call]: same number (or both hidden), started within [MATCH_WINDOW]; the closest. */
    private fun List<CallLogEntry>.closestMatch(call: CallRecord): CallLogEntry? =
        filter { entry -> (entry.timestamp - call.timestamp).absoluteValue <= MATCH_WINDOW && sameCaller(entry, call) }
            .minByOrNull { (it.timestamp - call.timestamp).absoluteValue }

    private fun sameCaller(
        entry: CallLogEntry,
        call: CallRecord,
    ): Boolean {
        val logged = normalizer.normalize(entry.number)
        val screened = call.number
        return if (logged == null ||
            screened == null
        ) {
            logged == screened
        } else {
            PhoneNumberMatching.sameNumber(logged, screened)
        }
    }

    companion object {
        /** A call log entry starts when the phone starts ringing, a moment after Knobly screened it. */
        val MATCH_WINDOW: Duration = 2.minutes

        /** Calls older than this are no longer looked for (the call log may have dropped them). */
        val LOOKBACK: Duration = 30.days
    }
}
