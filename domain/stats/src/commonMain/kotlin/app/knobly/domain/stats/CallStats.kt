package app.knobly.domain.stats

import app.knobly.domain.history.CallOutcome
import app.knobly.domain.history.CallRecord
import app.knobly.domain.history.RuleSnapshot
import kotlinx.datetime.LocalDateTime

/** Incoming calls of one [period]: counts per category, one count per bucket (oldest first), rejections per filter. */
data class CallStats(
    val period: StatsPeriod,
    val totals: CallCounts,
    val buckets: List<StatsBucket>,
    /** Rejected calls per filter, most first. */
    val byFilter: List<FilterCount>,
)

/** What a chart can show (docs/specs/call-statistics.md). */
enum class CallCategory {
    /** Every incoming call: rejected or not. */
    ALL,
    ANSWERED,
    MISSED,
    REJECTED,
}

/** Incoming calls by how they ended. [rang] are the calls that rang but whose outcome is unknown. */
data class CallCounts(
    val rejected: Int = 0,
    val answered: Int = 0,
    val missed: Int = 0,
    val rang: Int = 0,
) {
    val total: Int get() = rejected + answered + missed + rang

    fun of(category: CallCategory): Int =
        when (category) {
            CallCategory.ALL -> total
            CallCategory.ANSWERED -> answered
            CallCategory.MISSED -> missed
            CallCategory.REJECTED -> rejected
        }

    companion object {
        fun of(calls: List<CallRecord>): CallCounts {
            val byOutcome = calls.groupingBy { it.outcome }.eachCount()
            return CallCounts(
                rejected = byOutcome[CallOutcome.REJECTED] ?: 0,
                answered = byOutcome[CallOutcome.ANSWERED] ?: 0,
                missed = byOutcome[CallOutcome.MISSED] ?: 0,
                rang = byOutcome[CallOutcome.RANG] ?: 0,
            )
        }
    }
}

/** [start] is local time: the hour for [StatsPeriod.HOUR], midnight of the first day otherwise. */
data class StatsBucket(
    val start: LocalDateTime,
    val counts: CallCounts,
)

data class FilterCount(
    val filter: StatsFilter,
    val count: Int,
)

/** What rejected the calls. */
sealed interface StatsFilter {
    data object ContactsOnly : StatsFilter

    /** A custom rule, as it was on its most recent rejection. */
    data class Rule(
        val rule: RuleSnapshot,
    ) : StatsFilter

    /** Rules deleted before the call was recorded. */
    data object DeletedRule : StatsFilter

    /** Blocked by the system or another app (calls brought in from the call log). */
    data object BlockedBySystem : StatsFilter
}

/** What Home shows: calls rejected today and in the last 7 days, and the latest rejected call. */
data class CallsSummary(
    val rejectedToday: Int,
    val rejectedLastSevenDays: Int,
    val latestRejected: CallRecord?,
)
