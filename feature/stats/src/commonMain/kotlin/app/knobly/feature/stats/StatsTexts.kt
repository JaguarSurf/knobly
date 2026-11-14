package app.knobly.feature.stats

import androidx.compose.runtime.Composable
import app.knobly.domain.rules.call.CustomRuleType
import app.knobly.domain.stats.StatsFilter
import app.knobly.domain.stats.StatsPeriod
import app.knobly.feature.stats.resources.Res
import app.knobly.feature.stats.resources.stats_caption_day
import app.knobly.feature.stats.resources.stats_caption_hour
import app.knobly.feature.stats.resources.stats_caption_month
import app.knobly.feature.stats.resources.stats_caption_week
import app.knobly.feature.stats.resources.stats_caption_year
import app.knobly.feature.stats.resources.stats_filter_contacts
import app.knobly.feature.stats.resources.stats_filter_deleted
import app.knobly.feature.stats.resources.stats_hour_label
import app.knobly.feature.stats.resources.stats_months
import app.knobly.feature.stats.resources.stats_period_day
import app.knobly.feature.stats.resources.stats_period_hour
import app.knobly.feature.stats.resources.stats_period_month
import app.knobly.feature.stats.resources.stats_period_week
import app.knobly.feature.stats.resources.stats_period_year
import app.knobly.feature.stats.resources.stats_rule_hidden
import app.knobly.feature.stats.resources.stats_rule_prefix
import app.knobly.feature.stats.resources.stats_rule_regex
import app.knobly.feature.stats.resources.stats_week_label
import app.knobly.feature.stats.resources.stats_weekdays
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

internal val StatsPeriod.title: StringResource
    get() =
        when (this) {
            StatsPeriod.HOUR -> Res.string.stats_period_hour
            StatsPeriod.DAY -> Res.string.stats_period_day
            StatsPeriod.WEEK -> Res.string.stats_period_week
            StatsPeriod.MONTH -> Res.string.stats_period_month
            StatsPeriod.YEAR -> Res.string.stats_period_year
        }

private val StatsPeriod.caption: PluralStringResource
    get() =
        when (this) {
            StatsPeriod.HOUR -> Res.plurals.stats_caption_hour
            StatsPeriod.DAY -> Res.plurals.stats_caption_day
            StatsPeriod.WEEK -> Res.plurals.stats_caption_week
            StatsPeriod.MONTH -> Res.plurals.stats_caption_month
            StatsPeriod.YEAR -> Res.plurals.stats_caption_year
        }

/**
 * "rejeitadas nos últimos 7 dias", agreeing with [total]. Zero takes the plural ("0 rejeitadas"), as Brazilian
 * Portuguese does, although the CLDR rules for `pt` treat it as singular.
 */
@Composable
internal fun StatsPeriod.captionText(total: Int): String = pluralStringResource(caption, if (total == 0) 2 else total)

/** The x label of a bucket: "14h", "Qua", "9/11", "Nov" or "2026". */
@Composable
internal fun StatsPeriod.bucketLabels(starts: List<LocalDateTime>): List<String> {
    val weekdays = stringArrayResource(Res.array.stats_weekdays)
    val months = stringArrayResource(Res.array.stats_months)
    return starts.map { start ->
        when (this) {
            StatsPeriod.HOUR -> stringResource(Res.string.stats_hour_label, start.hour.toString())
            StatsPeriod.DAY -> weekdays[start.dayOfWeek.isoDayNumber - 1]
            StatsPeriod.WEEK -> stringResource(Res.string.stats_week_label, start.day, start.month.ordinal + 1)
            StatsPeriod.MONTH -> months[start.month.ordinal]
            StatsPeriod.YEAR -> start.year.toString()
        }
    }
}

/** "Fora dos contatos", "Telemarketing · 0303", "Começa com 0304"… */
@Composable
internal fun StatsFilter.text(): String =
    when (this) {
        StatsFilter.ContactsOnly -> {
            stringResource(Res.string.stats_filter_contacts)
        }

        StatsFilter.DeletedRule -> {
            stringResource(Res.string.stats_filter_deleted)
        }

        is StatsFilter.Rule -> {
            val value = rule.value
            when {
                rule.label.isNotBlank() && value.isNotBlank() -> "${rule.label} · $value"
                rule.label.isNotBlank() -> rule.label
                rule.type == CustomRuleType.PREFIX -> stringResource(Res.string.stats_rule_prefix, value)
                rule.type == CustomRuleType.HIDDEN_NUMBER -> stringResource(Res.string.stats_rule_hidden)
                rule.type == CustomRuleType.REGEX -> stringResource(Res.string.stats_rule_regex, value)
                else -> value
            }
        }
    }
