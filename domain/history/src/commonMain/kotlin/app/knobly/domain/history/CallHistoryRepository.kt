package app.knobly.domain.history

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** The rejected calls, stored only on the device. */
interface CallHistoryRepository {
    /** Newest first. */
    val calls: Flow<List<RejectedCall>>

    /** Stores [call] (its [RejectedCall.id] is ignored and assigned by the store). */
    suspend fun add(call: RejectedCall)

    suspend fun delete(id: Long)

    suspend fun clear()

    /** Retention: removes the calls older than [instant]. */
    suspend fun deleteOlderThan(instant: Instant)
}
