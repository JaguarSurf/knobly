package app.knobly.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.history.CallHistoryRepository
import app.knobly.domain.history.RejectedCall
import app.knobly.domain.history.RejectionReason
import app.knobly.domain.history.RuleSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

enum class HistoryFilter { ALL, CONTACTS_ONLY, RULES }

/** How a day header reads: "Hoje", "Ontem" or a date (with the year only when it is not the current one). */
sealed interface DayLabel {
    data object Today : DayLabel

    data object Yesterday : DayLabel

    data class Date(
        val date: LocalDate,
        val showYear: Boolean,
    ) : DayLabel
}

/** One rejected call, ready to show. */
data class HistoryEntry(
    val id: Long,
    /** Formatted for display; `null` when the number was hidden. */
    val number: String?,
    val reason: RejectionReason,
    val rule: RuleSnapshot?,
    val date: LocalDate,
    /** `HH:mm`, local time. */
    val time: String,
)

data class HistoryDay(
    val label: DayLabel,
    val entries: List<HistoryEntry>,
)

data class HistoryUiState(
    val loading: Boolean = true,
    val filter: HistoryFilter = HistoryFilter.ALL,
    /** Every stored call, whatever the filter (for the "clear history" confirmation). */
    val totalCount: Int = 0,
    val days: List<HistoryDay> = emptyList(),
    /** The entry shown in the details sheet. */
    val selected: HistoryEntry? = null,
    val confirmingClear: Boolean = false,
)

/** The History tab: rejected calls grouped by local day, newest first (docs/specs/call-history.md). */
class HistoryViewModel(
    private val history: CallHistoryRepository,
    private val normalizer: PhoneNumberNormalizer,
    private val clock: Clock,
    private val timeZone: TimeZone,
) : ViewModel() {
    private val filter = MutableStateFlow(HistoryFilter.ALL)
    private val selectedId = MutableStateFlow<Long?>(null)
    private val confirmingClear = MutableStateFlow(false)

    val state: StateFlow<HistoryUiState> =
        combine(history.calls, filter, selectedId, confirmingClear) { calls, filter, selectedId, confirming ->
            val entries = calls.map { it.toEntry() }
            val today = clock.now().toLocalDateTime(timeZone).date
            HistoryUiState(
                loading = false,
                filter = filter,
                totalCount = calls.size,
                days =
                    entries
                        .filter { it.matches(filter) }
                        .groupBy { it.date }
                        .map { (date, dayEntries) -> HistoryDay(date.label(today), dayEntries) },
                selected = entries.firstOrNull { it.id == selectedId },
                confirmingClear = confirming,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HistoryUiState())

    fun setFilter(value: HistoryFilter) = filter.update { value }

    fun select(id: Long) = selectedId.update { id }

    fun dismissDetails() = selectedId.update { null }

    fun delete(id: Long) {
        selectedId.update { null }
        viewModelScope.launch { history.delete(id) }
    }

    fun requestClear() = confirmingClear.update { true }

    fun cancelClear() = confirmingClear.update { false }

    fun clear() {
        confirmingClear.update { false }
        viewModelScope.launch { history.clear() }
    }

    private fun RejectedCall.toEntry(): HistoryEntry {
        val local = timestamp.toLocalDateTime(timeZone)
        return HistoryEntry(
            id = id,
            number = number?.let(normalizer::format),
            reason = reason,
            rule = rule,
            date = local.date,
            time = "${local.hour.twoDigits()}:${local.minute.twoDigits()}",
        )
    }
}

private fun HistoryEntry.matches(filter: HistoryFilter) =
    when (filter) {
        HistoryFilter.ALL -> true
        HistoryFilter.CONTACTS_ONLY -> reason == RejectionReason.CONTACTS_ONLY
        HistoryFilter.RULES -> reason != RejectionReason.CONTACTS_ONLY
    }

private fun LocalDate.label(today: LocalDate): DayLabel =
    when (this) {
        today -> DayLabel.Today
        today.minus(1, DateTimeUnit.DAY) -> DayLabel.Yesterday
        else -> DayLabel.Date(this, showYear = year != today.year)
    }

private fun Int.twoDigits() = toString().padStart(2, '0')
