package app.knobly.feature.stats

import app.knobly.domain.stats.CallCategory

/** Test tags of the Statistics tab, shared with UI tests. */
object StatsTestTags {
    const val TOTAL = "stats.total"
    const val CHART = "stats.chart"
    const val BY_TYPE = "stats.byType"
    const val BY_FILTER = "stats.byFilter"
    const val CALL_LOG = "stats.callLog"

    fun category(category: CallCategory) = "stats.category.${category.name}"
}
