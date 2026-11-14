package app.knobly.core.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonDocumentStoreTest {
    @Serializable
    data class Doc(
        val enabled: Boolean = false,
        val items: List<String> = emptyList(),
    )

    private fun tempFile(): File = File.createTempFile("knobly-doc", ".json").also { it.delete() }

    @Test
    fun `missing file reads as the default and updates are persisted`() =
        runTest {
            val file = tempFile()
            val store = createJsonDocumentStore(file.absolutePath, Doc.serializer(), Doc(), backgroundScope)
            assertEquals(Doc(), store.data.first())

            store.update { it.copy(enabled = true, items = listOf("0303")) }

            assertEquals(Doc(true, listOf("0303")), store.data.first())
            assertEquals(true, file.readText().contains("0303"))
        }

    @Test
    fun `corrupted file is replaced by the default`() =
        runTest {
            val file = tempFile().apply { writeText("{ not json") }
            val store = createJsonDocumentStore(file.absolutePath, Doc.serializer(), Doc(), backgroundScope)
            assertEquals(Doc(), store.data.first())
        }

    @Test
    fun `unknown keys from other app versions are ignored`() =
        runTest {
            val file = tempFile().apply { writeText("""{"enabled":true,"future":42}""") }
            val store = createJsonDocumentStore(file.absolutePath, Doc.serializer(), Doc(), backgroundScope)
            assertEquals(Doc(enabled = true), store.data.first())
        }
}
