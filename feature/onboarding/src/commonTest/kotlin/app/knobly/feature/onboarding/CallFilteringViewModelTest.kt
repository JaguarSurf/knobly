package app.knobly.feature.onboarding

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
class CallFilteringViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `enabling call filtering requests the role and updates the state`() =
        runTest {
            val viewModel = CallFilteringViewModel(FakeScreeningRoleController(roleHeld = false))
            viewModel.isRoleHeld.test {
                assertEquals(false, awaitItem())
                viewModel.requestRole()
                assertEquals(true, awaitItem())
            }
        }

    @Test
    fun `role revoked outside the app is reflected`() =
        runTest {
            val controller = FakeScreeningRoleController(roleHeld = true)
            val viewModel = CallFilteringViewModel(controller)
            viewModel.isRoleHeld.test {
                assertEquals(true, awaitItem())
                controller.setRoleHeld(false)
                assertEquals(false, awaitItem())
            }
        }

    @Test
    fun `platforms without the role are reported`() {
        val viewModel = CallFilteringViewModel(FakeScreeningRoleController(isRoleAvailable = false))
        assertFalse(viewModel.isRoleAvailable)
    }
}
