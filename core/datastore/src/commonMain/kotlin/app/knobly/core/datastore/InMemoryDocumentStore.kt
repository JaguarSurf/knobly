package app.knobly.core.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet

/** Non-persistent [DocumentStore], for tests and previews. */
class InMemoryDocumentStore<T>(
    initial: T,
) : DocumentStore<T> {
    private val state = MutableStateFlow(initial)
    override val data: Flow<T> = state

    /** The current document (test assertions). */
    val value: T get() = state.value

    override suspend fun update(transform: (T) -> T): T = state.updateAndGet(transform)

    /** Replaces the document without going through [update] (test setup). */
    fun set(value: T) = state.update { value }
}
