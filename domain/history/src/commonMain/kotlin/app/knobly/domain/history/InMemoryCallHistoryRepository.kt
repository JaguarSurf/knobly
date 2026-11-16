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

    /** Call log entries removed by the user, by key, with their time. */
    private val ignored = mutableMapOf<String, Instant>()

    override val calls: Flow<List<CallRecord>> = state

    /** The current calls (test assertions). */
    val value: List<CallRecord> get() = state.value

    override val latestRejected: Flow<CallRecord?> =
        state.map { calls -> calls.firstOrNull { it.outcome == CallOutcome.REJECTED } }

    override fun callsSince(instant: Instant): Flow<List<CallRecord>> =
        state.map { calls -> calls.filter { it.timestamp >= instant } }

    override suspend fun add(call: CallRecord) = addAll(listOf(call))

    override suspend fun addAll(calls: List<CallRecord>) =
        state.update { stored -> (stored + calls.map { it.copy(id = nextId++) }).sortedWith(newestFirst) }

    override suspend fun delete(id: Long) =
        state.update { calls ->
            calls.find { it.id == id }?.let { ignore(listOf(it)) }
            calls.filterNot { it.id == id }
        }

    override suspend fun clear() =
        state.update { calls ->
            ignore(calls)
            emptyList()
        }

    override suspend fun unlinked(instant: Instant): List<CallRecord> =
        state.value
            .filter { it.source == CallSource.KNOBLY && it.callLogKey == null && it.timestamp >= instant }
            .sortedWith(compareBy<CallRecord> { it.timestamp }.thenBy { it.id })

    override suspend fun knownCallLogKeys(instant: Instant): Set<String> =
        state.value
            .filter { it.timestamp >= instant }
            .mapNotNull { it.callLogKey }
            .toSet() +
            ignored.filterValues { it >= instant }.keys

    override suspend fun link(
        id: Long,
        callLogKey: String,
        outcome: CallOutcome?,
    ) = state.update { calls ->
        calls.map { if (it.id == id) it.copy(callLogKey = callLogKey, outcome = outcome ?: it.outcome) else it }
    }

    private fun ignore(calls: List<CallRecord>) =
        calls.forEach { call -> call.callLogKey?.let { ignored[it] = call.timestamp } }

    private companion object {
        val newestFirst = compareByDescending<CallRecord> { it.timestamp }.thenByDescending { it.id }
    }
}
