package app.knobly.core.datastore

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer

/**
 * Creates the app's persisted documents by name (`call_filters`, `app_settings`…). Platforms provide one factory
 * (the folder is platform specific); the shared code decides which documents exist.
 */
interface DocumentStoreFactory {
    /** Create each [name] once per process (e.g. in a Koin singleton). */
    fun <T> create(
        name: String,
        serializer: KSerializer<T>,
        default: T,
    ): DocumentStore<T>
}

/** Documents as JSON files in [directory]: `<directory>/<name>.json`. */
class JsonDocumentStoreFactory(
    private val directory: String,
    private val scope: CoroutineScope,
) : DocumentStoreFactory {
    override fun <T> create(
        name: String,
        serializer: KSerializer<T>,
        default: T,
    ): DocumentStore<T> = createJsonDocumentStore("$directory/$name.json", serializer, default, scope)
}

/** In-memory documents for tests and previews: [documents] by name; any other name starts at its default. */
class InMemoryDocumentStoreFactory(
    private val documents: Map<String, InMemoryDocumentStore<*>> = emptyMap(),
) : DocumentStoreFactory {
    @Suppress("UNCHECKED_CAST")
    override fun <T> create(
        name: String,
        serializer: KSerializer<T>,
        default: T,
    ): DocumentStore<T> = documents[name] as InMemoryDocumentStore<T>? ?: InMemoryDocumentStore(default)
}
