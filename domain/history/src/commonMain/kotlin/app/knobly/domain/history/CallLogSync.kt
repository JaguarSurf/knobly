package app.knobly.domain.history

import app.knobly.core.model.PhoneNumber
import app.knobly.core.phone.PhoneNumberMatching
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.port.CallLogAccess
import app.knobly.domain.port.CallLogEntry
import app.knobly.domain.port.CallLogType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Keeps the history in step with the system call log (docs/specs/call-history.md, ADR-0009). Each call log entry the
 * history does not know yet is either
 * - **linked** to the call Knobly screened (same number, started within [MATCH_WINDOW]): a call that rang gets its
 *   outcome, answered or missed; or
 * - **brought in** as a new call ([CallSource.CALL_LOG]) when Knobly did not screen it: before Knobly was installed,
 *   while it did not hold the call screening role, or blocked by the system or another app.
 *
 * Entries the user removed from the history are not brought in again, and neither are those older than the
 * retention. Runs when the app shows calls and after each screened call; does nothing without the permission.
 */
class CallLogSync(
    private val history: CallHistoryRepository,
    private val callLog: CallLogAccess,
    private val normalizer: PhoneNumberNormalizer,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher,
    private val retention: Duration = CallHistoryRecorder.RETENTION,
) {
    private val mutex = Mutex()

    /** Whether the platform has a call log to read (History and Statistics only offer it then). */
    val callLogAvailable: Boolean = callLog.isAvailable

    /** Whether Knobly may read the call log (History and Statistics offer to ask when it may not). */
    val callLogGranted: StateFlow<Boolean> = callLog.isGranted

    /** Asks for the call log permission and, if granted, syncs right away. */
    suspend fun requestCallLogAccess(): Boolean =
        callLog.requestAccess().also { granted ->
            if (granted) sync()
        }

    fun openCallLogSettings() = callLog.openAppSettings()

    suspend fun sync() {
        if (!callLog.isGranted.value) return
        mutex.withLock {
            val since = clock.now() - retention
            val entries = withContext(dispatcher) { callLog.incomingCalls(since) }
            val known = if (entries.isEmpty()) emptySet() else history.knownCallLogKeys(since)
            val fresh = entries.filter { it.key !in known }.distinctBy { it.key }.sortedBy { it.timestamp }
            if (fresh.isNotEmpty()) linkOrBringIn(fresh)
        }
    }

    /** [fresh]: call log entries the history does not know yet, oldest first. */
    private suspend fun linkOrBringIn(fresh: List<CallLogEntry>) {
        val unlinked = history.unlinked(fresh.first().timestamp - MATCH_WINDOW).toMutableList()
        val imported = mutableListOf<CallRecord>()
        fresh.forEach { entry ->
            val number = normalizer.normalize(entry.number)
            val screened = unlinked.closestMatch(entry, number)
            if (screened != null) {
                unlinked.remove(screened)
                history.link(screened.id, entry.key, entry.outcomeOf(screened))
            } else {
                imported += entry.toCallRecord(number)
            }
        }
        history.addAll(imported)
    }

    /**
     * The screened call of [entry]: same number (or both hidden), started within [MATCH_WINDOW], of the same kind (a
     * blocked entry matches a rejected call, an answered or missed one a call that rang); the closest.
     */
    private fun List<CallRecord>.closestMatch(
        entry: CallLogEntry,
        number: PhoneNumber?,
    ): CallRecord? =
        filter { call ->
            (entry.type == CallLogType.BLOCKED) == (call.outcome == CallOutcome.REJECTED) &&
                (entry.timestamp - call.timestamp).absoluteValue <= MATCH_WINDOW &&
                sameCaller(number, call.number)
        }.minByOrNull { (it.timestamp - entry.timestamp).absoluteValue }

    private fun sameCaller(
        logged: PhoneNumber?,
        screened: PhoneNumber?,
    ): Boolean =
        if (logged == null || screened == null) {
            logged == screened
        } else {
            PhoneNumberMatching.sameNumber(logged, screened)
        }

    /** How a screened call ended, from its call log entry (`null` for a rejected call: it stays rejected). */
    private fun CallLogEntry.outcomeOf(call: CallRecord): CallOutcome? =
        when {
            call.outcome == CallOutcome.REJECTED -> null
            type == CallLogType.ANSWERED -> CallOutcome.ANSWERED
            else -> CallOutcome.MISSED
        }

    private fun CallLogEntry.toCallRecord(number: PhoneNumber?) =
        CallRecord(
            id = 0,
            timestamp = timestamp,
            number = number,
            outcome =
                when (type) {
                    CallLogType.ANSWERED -> CallOutcome.ANSWERED
                    CallLogType.MISSED -> CallOutcome.MISSED
                    CallLogType.BLOCKED -> CallOutcome.REJECTED
                },
            reason = RejectionReason.BLOCKED_BY_SYSTEM.takeIf { type == CallLogType.BLOCKED },
            source = CallSource.CALL_LOG,
            callLogKey = key,
        )

    companion object {
        /** A call log entry starts when the phone starts ringing, a moment after Knobly screened it. */
        val MATCH_WINDOW: Duration = 2.minutes
    }
}
