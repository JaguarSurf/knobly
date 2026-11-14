package app.knobly.feature.home

import app.cash.turbine.test
import app.knobly.core.datastore.InMemoryDocumentStore
import app.knobly.core.model.PhoneNumber
import app.knobly.domain.history.InMemoryCallHistoryRepository
import app.knobly.domain.history.RejectedCall
import app.knobly.domain.history.RejectionReason
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.domain.rules.call.CallFilterSettingsRepository
import app.knobly.domain.stats.CallStatistics
import app.knobly.platform.fake.FakeScreeningRoleController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private fun repository() = CallFilterSettingsRepository(InMemoryDocumentStore(CallFilterSettings()))

    private val zone = TimeZone.of("America/Sao_Paulo")

    // Saturday 2026-11-14, 14:00 in São Paulo.
    private val clock =
        object : Clock {
            override fun now() = Instant.parse("2026-11-14T17:00:00Z")
        }

    private fun viewModel(
        role: FakeScreeningRoleController,
        settings: CallFilterSettingsRepository,
        history: InMemoryCallHistoryRepository = InMemoryCallHistoryRepository(),
    ) = HomeViewModel(role, settings, CallStatistics(history, clock) { zone }, { null }, clock, zone)

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
            val viewModel = viewModel(FakeScreeningRoleController(roleHeld = false), repository())
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
                viewModel(FakeScreeningRoleController(roleHeld = true), CallFilterSettingsRepository(store))
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
            viewModel(FakeScreeningRoleController(isRoleAvailable = false), repository()).isCallFilteringAvailable,
        )
    }

    @Test
    fun `rejected calls today and in the last 7 days and the latest one`() =
        runTest {
            fun call(
                id: Long,
                at: String,
            ) = RejectedCall(
                id,
                Instant.parse(at),
                PhoneNumber("03031234567", null, "3031234567"),
                RejectionReason.CONTACTS_ONLY,
            )
            val history =
                InMemoryCallHistoryRepository(
                    listOf(
                        // 14 Nov 11:32 and 13 Nov 23:30 local time.
                        call(1, "2026-11-14T14:32:00Z"),
                        call(2, "2026-11-14T02:30:00Z"),
                        call(3, "2026-11-10T12:00:00Z"),
                    ),
                )

            val activity = viewModel(FakeScreeningRoleController(roleHeld = true), repository(), history).activity.value

            assertEquals(1, activity.today)
            assertEquals(3, activity.lastSevenDays)
            assertEquals(LatestRejection("03031234567", LocalDate(2026, 11, 14), "11:32", daysAgo = 0), activity.latest)
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
