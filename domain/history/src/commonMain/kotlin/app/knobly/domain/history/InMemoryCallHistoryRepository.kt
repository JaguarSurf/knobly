package app.knobly.domain.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

/** Non-persistent [CallHistoryRepository], for UI tests and previews. */
class InMemoryCallHistoryRepository(
    initial: List<RejectedCall> = emptyList(),
) : CallHistoryRepository {
    private val state = MutableStateFlow(initial.sortedWith(newestFirst))
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override val calls: Flow<List<RejectedCall>> = state

    /** The current calls (test assertions). */
    val value: List<RejectedCall> get() = state.value

    override suspend fun add(call: RejectedCall) {
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

    private companion object {
        val newestFirst = compareByDescending<RejectedCall> { it.timestamp }.thenByDescending { it.id }
    }
}
