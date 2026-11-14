package app.knobly.core.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath

private val documentJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

/**
 * A [DocumentStore] persisted as a JSON file at [path] (absolute, platform specific). Unknown keys are ignored and a
 * corrupted file is replaced by [default], so older/newer app versions can read each other's files.
 *
 * Only one store may exist per file in a process: create it once (e.g. as a Koin singleton).
 */
fun <T> createJsonDocumentStore(
    path: String,
    serializer: KSerializer<T>,
    default: T,
    scope: CoroutineScope,
): DocumentStore<T> {
    val dataStore =
        DataStoreFactory.create(
            storage =
                OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = JsonOkioSerializer(serializer, default),
                    producePath = { path.toPath() },
                ),
            corruptionHandler = ReplaceFileCorruptionHandler { default },
            scope = scope,
        )
    return object : DocumentStore<T> {
        override val data: Flow<T> = dataStore.data

        override suspend fun update(transform: (T) -> T): T = dataStore.updateData { transform(it) }
    }
}

private class JsonOkioSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
) : OkioSerializer<T> {
    override suspend fun readFrom(source: BufferedSource): T =
        try {
            documentJson.decodeFromString(serializer, source.readUtf8())
        } catch (e: SerializationException) {
            throw androidx.datastore.core.CorruptionException("Cannot read document", e)
        } catch (e: IllegalArgumentException) {
            throw androidx.datastore.core.CorruptionException("Cannot read document", e)
        }

    override suspend fun writeTo(
        t: T,
        sink: BufferedSink,
    ) {
        sink.writeUtf8(documentJson.encodeToString(serializer, t))
    }
}
