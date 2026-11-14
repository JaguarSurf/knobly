package app.knobly.domain.history

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** The incoming calls screened by Knobly, stored only on the device. */
interface CallHistoryRepository {
    /** Newest first. */
    val calls: Flow<List<CallRecord>>

    /** The most recent rejected call, if any (Home). */
    val latestRejected: Flow<CallRecord?>

    /** The calls at or after [instant], newest first (statistics). */
    fun callsSince(instant: Instant): Flow<List<CallRecord>>

    /** Stores [call] (its [CallRecord.id] is ignored and assigned by the store). */
    suspend fun add(call: CallRecord)

    suspend fun delete(id: Long)

    suspend fun clear()

    /** Retention: removes the calls older than [instant]. */
    suspend fun deleteOlderThan(instant: Instant)

    /** Calls at or after [instant] that rang and whose outcome is not known yet, oldest first. */
    suspend fun pendingOutcomes(instant: Instant): List<CallRecord>

    /** Records how a call that rang ended: [CallOutcome.ANSWERED] or [CallOutcome.MISSED]. */
    suspend fun setOutcome(
        id: Long,
        outcome: CallOutcome,
    )
}
