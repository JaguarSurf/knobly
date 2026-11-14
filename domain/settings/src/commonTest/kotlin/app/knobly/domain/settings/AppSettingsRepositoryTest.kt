package app.knobly.domain.settings

import app.knobly.core.datastore.InMemoryDocumentStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsRepositoryTest {
    @Test
    fun `the theme follows the system until the user picks one`() =
        runTest(UnconfinedTestDispatcher()) {
            val store = InMemoryDocumentStore(AppSettings())
            val repository = AppSettingsRepository(store, backgroundScope)
            assertEquals(ThemeMode.SYSTEM, repository.themeMode.value)

            repository.setThemeMode(ThemeMode.DARK)

            assertEquals(ThemeMode.DARK, repository.themeMode.value)
            assertEquals(AppSettings(themeMode = ThemeMode.DARK), store.value)
        }
}

class AppSettingsFormatTest {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    @Test
    fun `stored format`() {
        assertEquals(
            AppSettings(ThemeMode.DARK),
            json.decodeFromString(AppSettings.serializer(), """{"themeMode":"dark","addedLater":1}"""),
        )
        assertEquals(AppSettings(), json.decodeFromString(AppSettings.serializer(), "{}"))
    }
}
