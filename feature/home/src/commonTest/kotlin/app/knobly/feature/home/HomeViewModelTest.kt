package app.knobly.feature.home

import app.cash.turbine.test
import app.knobly.core.datastore.InMemoryDocumentStore
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.domain.rules.call.CallFilterSettingsRepository
import app.knobly.platform.fake.FakeScreeningRoleController
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private fun repository() = CallFilterSettingsRepository(InMemoryDocumentStore(CallFilterSettings()))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `enabling call filtering requests the screening role`() =
        runTest {
            val viewModel = HomeViewModel(FakeScreeningRoleController(roleHeld = false), repository())
            viewModel.callsStatus.test {
                assertEquals(false, awaitItem().screeningOn)
                viewModel.enableCallFiltering()
                assertEquals(true, awaitItem().screeningOn)
            }
        }

    @Test
    fun `calls are protected only with the role and at least one active filter`() =
        runTest {
            val store = InMemoryDocumentStore(CallFilterSettings())
            val viewModel =
                HomeViewModel(FakeScreeningRoleController(roleHeld = true), CallFilterSettingsRepository(store))
            viewModel.callsStatus.test {
                assertEquals(CallsStatus(screeningOn = true, activeFilterCount = 0), awaitItem())
                assertFalse(CallsStatus(screeningOn = true, activeFilterCount = 0).isProtected)
                store.set(CallFilterSettings(contactsOnly = true))
                val protected = awaitItem()
                assertEquals(1, protected.activeFilterCount)
                assertTrue(protected.isProtected)
            }
        }

    @Test
    fun `platforms without the role report call filtering as unavailable`() {
        assertFalse(
            HomeViewModel(FakeScreeningRoleController(isRoleAvailable = false), repository()).isCallFilteringAvailable,
        )
    }
}

class DayPartTest {
    @Test
    fun `greeting follows the time of day`() {
        assertEquals(DayPart.EVENING, DayPart.fromHour(2))
        assertEquals(DayPart.MORNING, DayPart.fromHour(5))
        assertEquals(DayPart.MORNING, DayPart.fromHour(11))
        assertEquals(DayPart.AFTERNOON, DayPart.fromHour(12))
        assertEquals(DayPart.AFTERNOON, DayPart.fromHour(17))
        assertEquals(DayPart.EVENING, DayPart.fromHour(18))
    }
}
