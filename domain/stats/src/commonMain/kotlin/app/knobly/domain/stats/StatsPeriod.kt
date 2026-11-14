package app.knobly.domain.stats

/**
 * Time buckets for call statistics, aggregated in the local time zone. Each period shows a fixed number of buckets
 * ending with the current one (docs/specs/call-statistics.md).
 */
enum class StatsPeriod(
    val bucketCount: Int,
) {
    /** The last 24 hours. */
    HOUR(24),

    /** The last 7 days. */
    DAY(7),

    /** The last 8 weeks (weeks start on Monday). */
    WEEK(8),

    /** The last 12 months. */
    MONTH(12),

    /** The last 5 years (the history keeps 3, so older years are empty). */
    YEAR(5),
}
