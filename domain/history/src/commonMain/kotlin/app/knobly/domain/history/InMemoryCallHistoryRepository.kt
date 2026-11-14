package app.knobly.domain.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

/** Non-persistent [CallHistoryRepository], for UI tests and previews. */
class InMemoryCallHistoryRepository(
    initial: List<CallRecord> = emptyList(),
) : CallHistoryRepository {
    private val state = MutableStateFlow(initial.sortedWith(newestFirst))
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override val calls: Flow<List<CallRecord>> = state

    /** The current calls (test assertions). */
    val value: List<CallRecord> get() = state.value

    override val latestRejected: Flow<CallRecord?> =
        state.map { calls -> calls.firstOrNull { it.outcome == CallOutcome.REJECTED } }

    override fun callsSince(instant: Instant): Flow<List<CallRecord>> =
        state.map { calls -> calls.filter { it.timestamp >= instant } }

    override suspend fun add(call: CallRecord) {
        state.update { (it + call.copy(id = nextId++)).sortedWith(newestFirst) }
    }

    override suspend fun delete(id: Long) = state.update { calls -> calls.filterNot { it.id == id } }

    override suspend fun clear() = state.update { emptyList() }

    override suspend fun deleteOlderThan(instant: Instant) =
        state.update { calls ->
            calls.filter {
                it.timestamp >=
                    instant
            }
        }

    override suspend fun pendingOutcomes(instant: Instant): List<CallRecord> =
        state.value.filter { it.outcome == CallOutcome.RANG && it.timestamp >= instant }.sortedBy { it.timestamp }

    override suspend fun setOutcome(
        id: Long,
        outcome: CallOutcome,
    ) = state.update { calls ->
        calls.map {
            if (it.id == id &&
                it.outcome == CallOutcome.RANG
            ) {
                it.copy(outcome = outcome)
            } else {
                it
            }
        }
    }

    private companion object {
        val newestFirst = compareByDescending<CallRecord> { it.timestamp }.thenByDescending { it.id }
    }
}
