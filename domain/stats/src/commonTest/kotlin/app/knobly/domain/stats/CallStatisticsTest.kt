package app.knobly.domain.stats

import app.knobly.core.model.PhoneNumber
import app.knobly.domain.history.InMemoryCallHistoryRepository
import app.knobly.domain.history.RejectedCall
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
    ) = RejectedCall(id, time, PhoneNumber("03031234567", null, "3031234567"), reason, rule)

    private fun statistics(vararg calls: RejectedCall): CallStatistics {
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
                    // Outside the window.
                    call(5, at(2026, 11, 7, 23, 59)),
                ).stats(StatsPeriod.DAY).first()

            assertEquals(4, stats.total)
            assertEquals(listOf(1, 0, 0, 0, 0, 1, 2), stats.buckets.map { it.count })
            assertEquals(
                listOf(
                    // The newest call of the rule gives its label.
                    FilterCount(StatsFilter.Rule(tm), 2),
                    FilterCount(StatsFilter.ContactsOnly, 1),
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
            assertEquals(3, months.total)
            assertEquals(listOf(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1), months.buckets.map { it.count })

            val years = statistics.stats(StatsPeriod.YEAR).first()
            assertEquals(listOf(0, 0, 1, 1, 2), years.buckets.map { it.count })
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

            assertEquals(1, summary.today)
            assertEquals(2, summary.lastSevenDays)
            assertEquals(1L, summary.latest?.id)
        }

    @Test
    fun `no calls`() =
        runTest {
            val summary = statistics().summary().first()

            assertEquals(CallsSummary(today = 0, lastSevenDays = 0, latest = null), summary)
        }
}
