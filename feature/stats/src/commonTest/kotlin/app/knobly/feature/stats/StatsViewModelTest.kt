package app.knobly.feature.stats

import app.knobly.core.model.PhoneNumber
import app.knobly.domain.history.CallLogSync
import app.knobly.domain.history.CallOutcome
import app.knobly.domain.history.CallRecord
import app.knobly.domain.history.InMemoryCallHistoryRepository
import app.knobly.domain.history.RejectionReason
import app.knobly.domain.stats.CallCategory
import app.knobly.domain.stats.CallStatistics
import app.knobly.domain.stats.StatsPeriod
import app.knobly.platform.fake.FakeCallLogAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private val zone = TimeZone.of("America/Sao_Paulo")
    private val clock =
        object : Clock {
            override fun now() = Instant.parse("2026-11-14T17:00:00Z")
        }
    private val history = InMemoryCallHistoryRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun reject(at: String) =
        history.add(
            CallRecord(
                0,
                Instant.parse(at),
                PhoneNumber("03031234567", null, "3031234567"),
                CallOutcome.REJECTED,
                RejectionReason.CONTACTS_ONLY,
            ),
        )

    private fun TestScope.viewModel() =
        StatsViewModel(
            CallStatistics(history, clock) { zone },
            CallLogSync(history, callLog, { null }, clock, UnconfinedTestDispatcher(testScheduler)),
        )

    private val callLog = FakeCallLogAccess(granted = false)

    @Test
    fun `categories switch the chart and calls that only rang offer the call log`() =
        runTest {
            reject("2026-11-14T15:00:00Z")
            history.add(CallRecord(0, Instant.parse("2026-11-14T16:00:00Z"), null, CallOutcome.RANG))
            val viewModel = viewModel()
            assertEquals(
                2,
                viewModel.state.value.stats
                    ?.totals
                    ?.of(CallCategory.ALL),
            )
            assertTrue(viewModel.state.value.askCallLog)

            viewModel.setCategory(CallCategory.REJECTED)
            assertEquals(CallCategory.REJECTED, viewModel.state.value.category)
            assertEquals(
                1,
                viewModel.state.value.stats
                    ?.buckets
                    ?.last()
                    ?.counts
                    ?.of(CallCategory.REJECTED),
            )
        }

    @Test
    fun `opens on the last 8 weeks and switches periods`() =
        runTest {
            reject("2026-11-14T15:00:00Z")
            reject("2026-10-20T15:00:00Z")
            // Before the first of the 8 weeks (which starts on Monday 21 Sep).
            reject("2026-09-20T15:00:00Z")
            val viewModel = viewModel()

            assertEquals(StatsPeriod.WEEK, viewModel.state.value.period)
            assertEquals(
                2,
                viewModel.state.value.stats
                    ?.totals
                    ?.total,
            )
            assertEquals(
                8,
                viewModel.state.value.stats
                    ?.buckets
                    ?.size,
            )

            viewModel.setPeriod(StatsPeriod.MONTH)
            assertEquals(
                StatsPeriod.MONTH,
                viewModel.state.value.stats
                    ?.period,
            )
            // The last 12 months include 20 Sep.
            assertEquals(
                3,
                viewModel.state.value.stats
                    ?.totals
                    ?.total,
            )
        }

    @Test
    fun `new calls update the statistics`() =
        runTest {
            val viewModel = viewModel()
            assertEquals(
                0,
                viewModel.state.value.stats
                    ?.totals
                    ?.total,
            )

            reject("2026-11-14T16:00:00Z")

            assertEquals(
                1,
                viewModel.state.value.stats
                    ?.totals
                    ?.total,
            )
        }
}
