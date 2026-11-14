package app.knobly.domain.stats

import app.knobly.domain.history.RejectedCall
import app.knobly.domain.history.RuleSnapshot
import kotlinx.datetime.LocalDateTime

/** Rejected calls of one [period]: total, one count per bucket (oldest first) and the share of each filter. */
data class CallStats(
    val period: StatsPeriod,
    val total: Int,
    val buckets: List<StatsBucket>,
    /** Most rejections first. */
    val byFilter: List<FilterCount>,
)

/** [start] is local time: the hour for [StatsPeriod.HOUR], midnight of the first day otherwise. */
data class StatsBucket(
    val start: LocalDateTime,
    val count: Int,
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
}

/** What Home shows about calls: rejected today, in the last 7 days, and the latest one. */
data class CallsSummary(
    val today: Int,
    val lastSevenDays: Int,
    val latest: RejectedCall?,
)
