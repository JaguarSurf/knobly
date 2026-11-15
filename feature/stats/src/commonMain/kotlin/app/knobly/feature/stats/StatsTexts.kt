package app.knobly.feature.stats

import androidx.compose.runtime.Composable
import app.knobly.domain.rules.call.CustomRuleType
import app.knobly.domain.stats.CallCategory
import app.knobly.domain.stats.StatsFilter
import app.knobly.domain.stats.StatsPeriod
import app.knobly.feature.stats.resources.Res
import app.knobly.feature.stats.resources.stats_category_all
import app.knobly.feature.stats.resources.stats_category_answered
import app.knobly.feature.stats.resources.stats_category_missed
import app.knobly.feature.stats.resources.stats_category_rejected
import app.knobly.feature.stats.resources.stats_filter_contacts
import app.knobly.feature.stats.resources.stats_filter_deleted
import app.knobly.feature.stats.resources.stats_filter_system
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
import app.knobly.feature.stats.resources.stats_window_day
import app.knobly.feature.stats.resources.stats_window_hour
import app.knobly.feature.stats.resources.stats_window_month
import app.knobly.feature.stats.resources.stats_window_week
import app.knobly.feature.stats.resources.stats_window_year
import app.knobly.feature.stats.resources.stats_word_all
import app.knobly.feature.stats.resources.stats_word_answered
import app.knobly.feature.stats.resources.stats_word_missed
import app.knobly.feature.stats.resources.stats_word_rejected
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

private val StatsPeriod.window: StringResource
    get() =
        when (this) {
            StatsPeriod.HOUR -> Res.string.stats_window_hour
            StatsPeriod.DAY -> Res.string.stats_window_day
            StatsPeriod.WEEK -> Res.string.stats_window_week
            StatsPeriod.MONTH -> Res.string.stats_window_month
            StatsPeriod.YEAR -> Res.string.stats_window_year
        }

private val CallCategory.word: PluralStringResource
    get() =
        when (this) {
            CallCategory.ALL -> Res.plurals.stats_word_all
            CallCategory.ANSWERED -> Res.plurals.stats_word_answered
            CallCategory.MISSED -> Res.plurals.stats_word_missed
            CallCategory.REJECTED -> Res.plurals.stats_word_rejected
        }

internal val CallCategory.title: StringResource
    get() =
        when (this) {
            CallCategory.ALL -> Res.string.stats_category_all
            CallCategory.ANSWERED -> Res.string.stats_category_answered
            CallCategory.MISSED -> Res.string.stats_category_missed
            CallCategory.REJECTED -> Res.string.stats_category_rejected
        }

/**
 * "chamadas nos últimos 7 dias", "rejeitadas nas últimas 24 horas"…, agreeing with [total]. Zero takes the plural
 * ("0 rejeitadas"), as Brazilian Portuguese does, although the CLDR rules for `pt` treat it as singular.
 */
@Composable
internal fun captionText(
    period: StatsPeriod,
    category: CallCategory,
    total: Int,
): String = pluralStringResource(category.word, if (total == 0) 2 else total) + " " + stringResource(period.window)

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

        StatsFilter.BlockedBySystem -> {
            stringResource(Res.string.stats_filter_system)
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
