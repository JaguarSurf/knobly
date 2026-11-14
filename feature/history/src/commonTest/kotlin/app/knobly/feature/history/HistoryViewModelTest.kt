package app.knobly.feature.history

import app.knobly.core.model.PhoneNumber
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.history.InMemoryCallHistoryRepository
import app.knobly.domain.history.RejectedCall
import app.knobly.domain.history.RejectionReason
import app.knobly.domain.history.RuleSnapshot
import app.knobly.domain.rules.call.CustomRuleType
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    // 2026-11-14 14:00 in São Paulo (UTC-3).
    private val now = Instant.parse("2026-11-14T17:00:00Z")
    private val clock =
        object : Clock {
            override fun now() = now
        }
    private val timeZone = TimeZone.of("America/Sao_Paulo")
    private val normalizer = PhoneNumberNormalizer { null }
    private val tm = RuleSnapshot("tm", CustomRuleType.PREFIX, "0303", "Telemarketing")

    private fun call(
        id: Long,
        at: String,
        reason: RejectionReason,
        raw: String? = "03031234567",
    ) = RejectedCall(
        id = id,
        timestamp = Instant.parse(at),
        number = raw?.let { PhoneNumber(it, null, it) },
        reason = reason,
        rule = tm.takeIf { reason == RejectionReason.CUSTOM_RULE },
    )

    private val history =
        InMemoryCallHistoryRepository(
            listOf(
                // 14 Nov, 11:32 local
                call(1, "2026-11-14T14:32:00Z", RejectionReason.CUSTOM_RULE),
                // 14 Nov, 00:10 local (03:10 UTC): still "today" in São Paulo
                call(2, "2026-11-14T03:10:00Z", RejectionReason.CONTACTS_ONLY, raw = null),
                // 13 Nov, 23:59 local (14 Nov in UTC): "yesterday" in São Paulo
                call(3, "2026-11-14T02:59:00Z", RejectionReason.CONTACTS_ONLY),
                // Last year
                call(4, "2025-12-31T15:00:00Z", RejectionReason.UNKNOWN),
            ),
        )

    private fun viewModel() = HistoryViewModel(history, normalizer, clock, timeZone)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `calls are grouped by local day with the newest first`() =
        runTest {
            val state = viewModel().state.value

            assertFalse(state.loading)
            assertEquals(4, state.totalCount)
            assertEquals(
                listOf(DayLabel.Today, DayLabel.Yesterday, DayLabel.Date(LocalDate(2025, 12, 31), showYear = true)),
                state.days.map { it.label },
            )
            assertEquals(listOf(1L, 2L), state.days[0].entries.map { it.id })
            assertEquals(listOf("11:32", "00:10"), state.days[0].entries.map { it.time })
            assertEquals(
                "23:59",
                state.days[1]
                    .entries
                    .single()
                    .time,
            )
            assertNull(state.days[0].entries[1].number)
            assertEquals("03031234567", state.days[0].entries[0].number)
        }

    @Test
    fun `filters split contacts only from rules`() =
        runTest {
            val viewModel = viewModel()

            viewModel.setFilter(HistoryFilter.CONTACTS_ONLY)
            assertEquals(
                listOf(2L, 3L),
                viewModel.state.value.days
                    .flatMap { day -> day.entries.map { it.id } },
            )

            viewModel.setFilter(HistoryFilter.RULES)
            assertEquals(
                listOf(1L, 4L),
                viewModel.state.value.days
                    .flatMap { day -> day.entries.map { it.id } },
            )
            assertEquals(4, viewModel.state.value.totalCount)
        }

    @Test
    fun `details can delete one call`() =
        runTest {
            val viewModel = viewModel()

            viewModel.select(1)
            assertEquals(
                tm,
                viewModel.state.value.selected
                    ?.rule,
            )

            viewModel.delete(1)
            assertNull(viewModel.state.value.selected)
            assertEquals(
                listOf(4L, 3L, 2L),
                history.value
                    .map { it.id }
                    .sorted()
                    .reversed(),
            )
        }

    @Test
    fun `clearing asks for confirmation first`() =
        runTest {
            val viewModel = viewModel()

            viewModel.requestClear()
            assertTrue(viewModel.state.value.confirmingClear)
            viewModel.cancelClear()
            assertFalse(viewModel.state.value.confirmingClear)
            assertEquals(4, history.value.size)

            viewModel.requestClear()
            viewModel.clear()
            assertFalse(viewModel.state.value.confirmingClear)
            assertTrue(history.value.isEmpty())
            assertEquals(0, viewModel.state.value.totalCount)
        }
}
