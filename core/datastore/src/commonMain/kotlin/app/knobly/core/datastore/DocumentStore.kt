package app.knobly.core.datastore

import kotlinx.coroutines.flow.Flow

/**
 * A single persisted document (e.g. the call filter settings), observed as a [Flow] and updated atomically.
 * Implementations: [createJsonDocumentStore] (a JSON file, via DataStore) and [InMemoryDocumentStore] (tests).
 */
interface DocumentStore<T> {
    val data: Flow<T>

    /** Atomically replaces the document with [transform] applied to the current value; returns the new value. */
    suspend fun update(transform: (T) -> T): T
}
