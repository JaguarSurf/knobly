package app.knobly.feature.history

import androidx.compose.runtime.Composable
import app.knobly.domain.history.CallOutcome
import app.knobly.domain.history.RejectionReason
import app.knobly.domain.history.RuleSnapshot
import app.knobly.domain.rules.call.CustomRuleType
import app.knobly.feature.history.resources.Res
import app.knobly.feature.history.resources.history_date
import app.knobly.feature.history.resources.history_date_year
import app.knobly.feature.history.resources.history_months
import app.knobly.feature.history.resources.history_reason_contacts
import app.knobly.feature.history.resources.history_reason_unknown
import app.knobly.feature.history.resources.history_rule_exact
import app.knobly.feature.history.resources.history_rule_hidden
import app.knobly.feature.history.resources.history_rule_prefix
import app.knobly.feature.history.resources.history_rule_regex
import app.knobly.feature.history.resources.history_status_answered
import app.knobly.feature.history.resources.history_status_missed
import app.knobly.feature.history.resources.history_status_rang
import app.knobly.feature.history.resources.history_status_rejected
import app.knobly.feature.history.resources.history_today
import app.knobly.feature.history.resources.history_yesterday
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/** "Atendida", "Perdida", "Recebida" or "Rejeitada · Telemarketing · começa com 0303". */
@Composable
internal fun HistoryEntry.statusText(): String =
    when (outcome) {
        CallOutcome.ANSWERED -> stringResource(Res.string.history_status_answered)
        CallOutcome.MISSED -> stringResource(Res.string.history_status_missed)
        CallOutcome.RANG -> stringResource(Res.string.history_status_rang)
        CallOutcome.REJECTED -> stringResource(Res.string.history_status_rejected, reasonText())
    }

/** Why the call was rejected, e.g. "Telemarketing · começa com 0303" or "Fora dos contatos". */
@Composable
internal fun HistoryEntry.reasonText(): String =
    when (reason) {
        RejectionReason.CONTACTS_ONLY -> stringResource(Res.string.history_reason_contacts)
        RejectionReason.CUSTOM_RULE -> rule?.describe() ?: stringResource(Res.string.history_reason_unknown)
        RejectionReason.UNKNOWN, null -> stringResource(Res.string.history_reason_unknown)
    }

@Composable
private fun RuleSnapshot.describe(): String {
    val what =
        when (type) {
            CustomRuleType.PREFIX -> stringResource(Res.string.history_rule_prefix, value)
            CustomRuleType.EXACT_NUMBER -> stringResource(Res.string.history_rule_exact)
            CustomRuleType.HIDDEN_NUMBER -> stringResource(Res.string.history_rule_hidden)
            CustomRuleType.REGEX -> stringResource(Res.string.history_rule_regex, value)
        }
    return if (label.isBlank()) what else "$label · ${what.replaceFirstChar { it.lowercase() }}"
}

@Composable
internal fun DayLabel.text(): String =
    when (this) {
        DayLabel.Today -> stringResource(Res.string.history_today)
        DayLabel.Yesterday -> stringResource(Res.string.history_yesterday)
        is DayLabel.Date -> date.text(showYear)
    }

/** "14 de novembro" / "November 14", with the year when asked. */
@Composable
internal fun LocalDate.text(showYear: Boolean): String {
    val month = stringArrayResource(Res.array.history_months)[month.ordinal]
    return if (showYear) {
        stringResource(Res.string.history_date_year, day, month, year)
    } else {
        stringResource(Res.string.history_date, day, month)
    }
}
