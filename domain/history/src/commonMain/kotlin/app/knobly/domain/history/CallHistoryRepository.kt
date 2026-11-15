package app.knobly.domain.history

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * The incoming calls in the history (screened by Knobly or brought in from the call log), stored only on the device.
 */
interface CallHistoryRepository {
    /** Newest first. */
    val calls: Flow<List<CallRecord>>

    /** The most recent rejected call, if any (Home). */
    val latestRejected: Flow<CallRecord?>

    /** The calls at or after [instant], newest first (statistics). */
    fun callsSince(instant: Instant): Flow<List<CallRecord>>

    /** Stores [call] (its [CallRecord.id] is ignored and assigned by the store). */
    suspend fun add(call: CallRecord)

    /** Stores [calls] at once (their ids are ignored). */
    suspend fun addAll(calls: List<CallRecord>)

    /** Deletes one call; its call log entry, if any, is not brought in again. */
    suspend fun delete(id: Long)

    /** Deletes every call; their call log entries are not brought in again. */
    suspend fun clear()

    /** Retention: removes the calls older than [instant]. */
    suspend fun deleteOlderThan(instant: Instant)

    /** Calls screened by Knobly at or after [instant] that are not linked to a call log entry yet, oldest first. */
    suspend fun unlinked(instant: Instant): List<CallRecord>

    /** Keys of the call log entries at or after [instant] already in the history, or removed from it by the user. */
    suspend fun knownCallLogKeys(instant: Instant): Set<String>

    /**
     * Links a screened call to its call log entry [callLogKey]; for a call that rang, [outcome] is how it ended
     * ([CallOutcome.ANSWERED] or [CallOutcome.MISSED]; `null` keeps the current one).
     */
    suspend fun link(
        id: Long,
        callLogKey: String,
        outcome: CallOutcome?,
    )
}
