package app.knobly.feature.home

import app.knobly.core.datastore.InMemoryDocumentStore
import app.knobly.domain.settings.AppSettings
import app.knobly.domain.settings.AppSettingsRepository
import app.knobly.domain.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the theme choice is saved`() =
        runTest(UnconfinedTestDispatcher()) {
            val store = InMemoryDocumentStore(AppSettings())
            val viewModel = ThemeViewModel(AppSettingsRepository(store, backgroundScope))
            assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

            viewModel.setThemeMode(ThemeMode.LIGHT)

            assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
            assertEquals(ThemeMode.LIGHT, store.value.themeMode)
        }
}
