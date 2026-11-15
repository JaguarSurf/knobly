package app.knobly.domain.stats

import app.knobly.core.model.PhoneNumber
import app.knobly.domain.history.CallOutcome
import app.knobly.domain.history.CallRecord
import app.knobly.domain.history.InMemoryCallHistoryRepository
import app.knobly.domain.history.RejectionReason
import app.knobly.domain.history.RuleSnapshot
import app.knobly.domain.rules.call.CustomRuleType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class CallStatisticsTest {
    private val saoPaulo = TimeZone.of("America/Sao_Paulo")

    // Saturday 2026-11-14, 14:20 in São Paulo (UTC-3, no DST).
    private val now = LocalDateTime(2026, 11, 14, 14, 20)
    private val tm = RuleSnapshot("tm", CustomRuleType.PREFIX, "0303", "Telemarketing")

    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12,
        minute: Int = 0,
    ) = LocalDateTime(year, month, day, hour, minute).toInstant(saoPaulo)

    private fun call(
        id: Long,
        time: Instant,
        reason: RejectionReason = RejectionReason.CONTACTS_ONLY,
        rule: RuleSnapshot? = null,
    ) = CallRecord(id, time, PhoneNumber("03031234567", null, "3031234567"), CallOutcome.REJECTED, reason, rule)

    private fun statistics(vararg calls: CallRecord): CallStatistics {
        val clock =
            object : Clock {
                override fun now() = now.toInstant(saoPaulo)
            }
        return CallStatistics(InMemoryCallHistoryRepository(calls.toList()), clock) { saoPaulo }
    }

    @Test
    fun `bucket windows end with the current bucket`() {
        fun starts(period: StatsPeriod) = bucketStarts(period, now, saoPaulo)

        assertEquals(LocalDateTime(2026, 11, 13, 15, 0), starts(StatsPeriod.HOUR).first())
        assertEquals(LocalDateTime(2026, 11, 14, 14, 0), starts(StatsPeriod.HOUR).last())
        assertEquals(24, starts(StatsPeriod.HOUR).size)
        assertEquals(LocalDateTime(2026, 11, 8, 0, 0), starts(StatsPeriod.DAY).first())
        assertEquals(LocalDateTime(2026, 11, 14, 0, 0), starts(StatsPeriod.DAY).last())
        // Weeks start on Monday: the current week started on 2026-11-09.
        assertEquals(LocalDateTime(2026, 9, 21, 0, 0), starts(StatsPeriod.WEEK).first())
        assertEquals(LocalDateTime(2026, 11, 9, 0, 0), starts(StatsPeriod.WEEK).last())
        assertEquals(LocalDateTime(2025, 12, 1, 0, 0), starts(StatsPeriod.MONTH).first())
        assertEquals(LocalDateTime(2026, 11, 1, 0, 0), starts(StatsPeriod.MONTH).last())
        assertEquals(listOf(2022, 2023, 2024, 2025, 2026), starts(StatsPeriod.YEAR).map { it.year })
    }

    @Test
    fun `hours follow the real clock across a DST change`() {
        // New York falls back on 2026-11-01 at 02:00 (01:00-01:59 happens twice).
        val newYork = TimeZone.of("America/New_York")
        val hours = bucketStarts(StatsPeriod.HOUR, LocalDateTime(2026, 11, 1, 12, 30), newYork)

        assertEquals(23, hours.size)
        assertEquals(hours.distinct(), hours)
        assertEquals(LocalDateTime(2026, 11, 1, 12, 0), hours.last())
    }

    @Test
    fun `counts per day with the total and the filters of the last 7 days`() =
        runTest {
            val stats =
                statistics(
                    call(1, at(2026, 11, 14, 9), RejectionReason.CUSTOM_RULE, tm),
                    call(2, at(2026, 11, 14, 0, 5)),
                    // 13 Nov at 23:30 local is already 14 Nov in UTC: it must count on the 13th.
                    call(3, at(2026, 11, 13, 23, 30), RejectionReason.CUSTOM_RULE, tm.copy(label = "Old label")),
                    call(4, at(2026, 11, 8, 0, 1), RejectionReason.UNKNOWN),
                    // From the call log: blocked by the system or another app.
                    call(6, at(2026, 11, 10), RejectionReason.BLOCKED_BY_SYSTEM),
                    // Outside the window.
                    call(5, at(2026, 11, 7, 23, 59)),
                ).stats(StatsPeriod.DAY).first()

            assertEquals(5, stats.totals.total)
            assertEquals(listOf(1, 0, 1, 0, 0, 1, 2), stats.buckets.map { it.counts.total })
            assertEquals(
                listOf(
                    // The newest call of the rule gives its label.
                    FilterCount(StatsFilter.Rule(tm), 2),
                    FilterCount(StatsFilter.ContactsOnly, 1),
                    FilterCount(StatsFilter.BlockedBySystem, 1),
                    FilterCount(StatsFilter.DeletedRule, 1),
                ),
                stats.byFilter,
            )
        }

    @Test
    fun `months and years`() =
        runTest {
            val statistics =
                statistics(
                    call(1, at(2026, 11, 1)),
                    call(2, at(2026, 1, 31)),
                    call(3, at(2025, 12, 1)),
                    call(4, at(2024, 6, 1)),
                )

            val months = statistics.stats(StatsPeriod.MONTH).first()
            assertEquals(3, months.totals.total)
            assertEquals(listOf(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1), months.buckets.map { it.counts.total })

            val years = statistics.stats(StatsPeriod.YEAR).first()
            assertEquals(listOf(0, 0, 1, 1, 2), years.buckets.map { it.counts.total })
        }

    @Test
    fun `summary for home`() =
        runTest {
            val summary =
                statistics(
                    call(1, at(2026, 11, 14, 9)),
                    call(2, at(2026, 11, 10)),
                    call(3, at(2026, 10, 1)),
                ).summary().first()

            assertEquals(1, summary.rejectedToday)
            assertEquals(2, summary.rejectedLastSevenDays)
            assertEquals(1L, summary.latestRejected?.id)
        }

    @Test
    fun `no calls`() =
        runTest {
            val summary = statistics().summary().first()

            assertEquals(CallsSummary(rejectedToday = 0, rejectedLastSevenDays = 0, latestRejected = null), summary)
        }

    @Test
    fun `categories per bucket and in total with filters counting only rejections`() =
        runTest {
            fun allowed(
                id: Long,
                time: Instant,
                outcome: CallOutcome,
            ) = CallRecord(id, time, PhoneNumber("11912345678", null, "11912345678"), outcome)
            val stats =
                statistics(
                    call(1, at(2026, 11, 14, 9), RejectionReason.CUSTOM_RULE, tm),
                    allowed(2, at(2026, 11, 14, 10), CallOutcome.ANSWERED),
                    allowed(3, at(2026, 11, 14, 11), CallOutcome.MISSED),
                    allowed(4, at(2026, 11, 13, 11), CallOutcome.ANSWERED),
                    allowed(5, at(2026, 11, 13, 12), CallOutcome.RANG),
                ).stats(StatsPeriod.DAY).first()

            assertEquals(CallCounts(rejected = 1, answered = 2, missed = 1, rang = 1), stats.totals)
            assertEquals(5, stats.totals.of(CallCategory.ALL))
            assertEquals(CallCounts(rejected = 1, answered = 1, missed = 1), stats.buckets.last().counts)
            assertEquals(listOf(0, 0, 0, 0, 0, 1, 1), stats.buckets.map { it.counts.of(CallCategory.ANSWERED) })
            assertEquals(listOf(FilterCount(StatsFilter.Rule(tm), 1)), stats.byFilter)
        }

    @Test
    fun `home counts only rejections`() =
        runTest {
            val summary =
                statistics(
                    call(1, at(2026, 11, 14, 9)),
                    CallRecord(
                        2,
                        at(2026, 11, 14, 10),
                        PhoneNumber("11912345678", null, "11912345678"),
                        CallOutcome.ANSWERED,
                    ),
                ).summary().first()

            assertEquals(1, summary.rejectedToday)
            assertEquals(1L, summary.latestRejected?.id)
        }
}
