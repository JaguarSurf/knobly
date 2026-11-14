package app.knobly.domain.stats

import app.knobly.domain.history.CallHistoryRepository
import app.knobly.domain.history.CallOutcome
import app.knobly.domain.history.CallRecord
import app.knobly.domain.history.RejectionReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Statistics of the incoming calls (rejected, answered, missed), computed from the history in the local time zone
 * (so days, weeks and DST follow the phone's clock). A few thousand calls per year at most: they are aggregated in
 * memory, not in SQL.
 */
class CallStatistics(
    private val history: CallHistoryRepository,
    private val clock: Clock,
    private val timeZone: () -> TimeZone,
) {
    /** Recomputed whenever a call is recorded or deleted. The window is fixed when collection starts. */
    fun stats(period: StatsPeriod): Flow<CallStats> =
        flow {
            val zone = timeZone()
            val buckets = bucketStarts(period, clock.now().toLocalDateTime(zone), zone)
            emitAll(
                history
                    .callsSince(buckets.first().toInstant(zone))
                    .map { calls -> aggregate(period, buckets, calls, zone) },
            )
        }

    fun summary(): Flow<CallsSummary> =
        combine(stats(StatsPeriod.DAY), history.latestRejected) { days, latest ->
            CallsSummary(
                rejectedToday =
                    days.buckets
                        .last()
                        .counts.rejected,
                rejectedLastSevenDays = days.totals.rejected,
                latestRejected = latest,
            )
        }

    private fun aggregate(
        period: StatsPeriod,
        buckets: List<LocalDateTime>,
        calls: List<CallRecord>,
        zone: TimeZone,
    ): CallStats {
        val window = buckets.toSet()
        val byBucket =
            calls
                .groupBy {
                    bucketOf(
                        period,
                        it.timestamp.toLocalDateTime(zone),
                    )
                }.filterKeys { it in window }
        val inWindow = calls.filter { bucketOf(period, it.timestamp.toLocalDateTime(zone)) in window }
        return CallStats(
            period = period,
            totals = CallCounts.of(inWindow),
            buckets = buckets.map { StatsBucket(it, CallCounts.of(byBucket[it].orEmpty())) },
            byFilter = byFilter(inWindow.filter { it.outcome == CallOutcome.REJECTED }),
        )
    }

    /** Calls are newest first, so the first call of each rule carries its most recent label. */
    private fun byFilter(calls: List<CallRecord>): List<FilterCount> =
        calls
            .groupBy { call ->
                when (call.reason) {
                    RejectionReason.CONTACTS_ONLY -> FilterKey.ContactsOnly
                    RejectionReason.CUSTOM_RULE -> call.rule?.let { FilterKey.Rule(it.id) } ?: FilterKey.Deleted
                    RejectionReason.UNKNOWN, null -> FilterKey.Deleted
                }
            }.map { (key, group) ->
                val filter =
                    when (key) {
                        FilterKey.ContactsOnly -> StatsFilter.ContactsOnly
                        FilterKey.Deleted -> StatsFilter.DeletedRule
                        is FilterKey.Rule -> StatsFilter.Rule(requireNotNull(group.first().rule))
                    }
                FilterCount(filter, group.size)
            }.sortedByDescending { it.count }

    private sealed interface FilterKey {
        data object ContactsOnly : FilterKey

        data object Deleted : FilterKey

        data class Rule(
            val id: String,
        ) : FilterKey
    }
}

/** The local start of every bucket of [period] (oldest first), ending with the one that contains [now]. */
internal fun bucketStarts(
    period: StatsPeriod,
    now: LocalDateTime,
    zone: TimeZone,
): List<LocalDateTime> {
    val today = now.date
    val count = period.bucketCount
    return when (period) {
        StatsPeriod.HOUR -> {
            // Step in real time, so a DST change gives 23 or 25 distinct local hours instead of wrong ones.
            val current = bucketOf(period, now).toInstant(zone)
            (count - 1 downTo 0).map { bucketOf(period, (current - it.hours).toLocalDateTime(zone)) }.distinct()
        }

        StatsPeriod.DAY -> {
            (count - 1 downTo 0).map { today.minus(it, DateTimeUnit.DAY).atMidnight() }
        }

        StatsPeriod.WEEK -> {
            val monday = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
            (count - 1 downTo 0).map { monday.minus(it, DateTimeUnit.WEEK).atMidnight() }
        }

        StatsPeriod.MONTH -> {
            val first = LocalDate(today.year, today.month, 1)
            (count - 1 downTo 0).map { first.minus(it, DateTimeUnit.MONTH).atMidnight() }
        }

        StatsPeriod.YEAR -> {
            (count - 1 downTo 0).map { LocalDate(today.year - it, 1, 1).atMidnight() }
        }
    }
}

/** The local start of the bucket of [period] that contains [time]. */
internal fun bucketOf(
    period: StatsPeriod,
    time: LocalDateTime,
): LocalDateTime =
    when (period) {
        StatsPeriod.HOUR -> LocalDateTime(time.date, LocalTime(time.hour, 0))
        StatsPeriod.DAY -> time.date.atMidnight()
        StatsPeriod.WEEK -> time.date.minus(time.date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY).atMidnight()
        StatsPeriod.MONTH -> LocalDate(time.year, time.month, 1).atMidnight()
        StatsPeriod.YEAR -> LocalDate(time.year, 1, 1).atMidnight()
    }

private fun LocalDate.atMidnight() = LocalDateTime(this, LocalTime(0, 0))
