package app.knobly.feature.home

import app.cash.turbine.test
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
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
            val viewModel = HomeViewModel(FakeScreeningRoleController(roleHeld = false))
            viewModel.isCallFilteringOn.test {
                assertEquals(false, awaitItem())
                viewModel.enableCallFiltering()
                assertEquals(true, awaitItem())
            }
        }

    @Test
    fun `platforms without the role report call filtering as unavailable`() {
        assertFalse(HomeViewModel(FakeScreeningRoleController(isRoleAvailable = false)).isCallFilteringAvailable)
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
