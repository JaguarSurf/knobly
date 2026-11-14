package app.knobly.feature.stats

import app.knobly.core.model.PhoneNumber
import app.knobly.domain.history.InMemoryCallHistoryRepository
import app.knobly.domain.history.RejectedCall
import app.knobly.domain.history.RejectionReason
import app.knobly.domain.stats.CallStatistics
import app.knobly.domain.stats.StatsPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
            RejectedCall(
                0,
                Instant.parse(at),
                PhoneNumber("03031234567", null, "3031234567"),
                RejectionReason.CONTACTS_ONLY,
            ),
        )

    @Test
    fun `opens on the last 7 days and switches periods`() =
        runTest {
            reject("2026-11-14T15:00:00Z")
            reject("2026-10-20T15:00:00Z")
            val viewModel = StatsViewModel(CallStatistics(history, clock) { zone })

            assertEquals(StatsPeriod.DAY, viewModel.state.value.period)
            assertEquals(
                1,
                viewModel.state.value.stats
                    ?.total,
            )
            assertEquals(
                7,
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
            assertEquals(
                2,
                viewModel.state.value.stats
                    ?.total,
            )
        }

    @Test
    fun `new calls update the statistics`() =
        runTest {
            val viewModel = StatsViewModel(CallStatistics(history, clock) { zone })
            assertEquals(
                0,
                viewModel.state.value.stats
                    ?.total,
            )

            reject("2026-11-14T16:00:00Z")

            assertEquals(
                1,
                viewModel.state.value.stats
                    ?.total,
            )
        }
}
