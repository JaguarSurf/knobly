package app.knobly.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.history.CallHistoryRepository
import app.knobly.domain.history.CallLogSync
import app.knobly.domain.history.CallOutcome
import app.knobly.domain.history.CallRecord
import app.knobly.domain.history.CallSource
import app.knobly.domain.history.RejectionReason
import app.knobly.domain.history.RuleSnapshot
import app.knobly.domain.rules.call.ContactDirectory
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

/** The History chips: every incoming call, or one outcome. */
enum class HistoryFilter { ALL, ANSWERED, MISSED, REJECTED }

/**
 * Whether to offer the call log permission: to bring in the calls Knobly did not screen and tell answered from missed
 * calls.
 */
enum class CallLogPrompt {
    /** Granted, or the platform has no call log (iOS). */
    NONE,

    /** Not granted: offer to allow it. */
    ASK,

    /** The user refused: point to the app settings. */
    DENIED,
}

/** How a day header reads: "Hoje", "Ontem" or a date (with the year only when it is not the current one). */
sealed interface DayLabel {
    data object Today : DayLabel

    data object Yesterday : DayLabel

    data class Date(
        val date: LocalDate,
        val showYear: Boolean,
    ) : DayLabel
}

/** One incoming call, ready to show. */
data class HistoryEntry(
    val id: Long,
    /** Formatted for display; `null` when the number was hidden. */
    val number: String?,
    /** The contact's name, when the number is saved and the contacts can be read. */
    val name: String?,
    val outcome: CallOutcome,
    val reason: RejectionReason?,
    val rule: RuleSnapshot?,
    /** Brought in from the system call log (Knobly did not screen it). */
    val fromCallLog: Boolean,
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
    val callLogPrompt: CallLogPrompt = CallLogPrompt.NONE,
)

/** The History tab: incoming calls grouped by local day, newest first (docs/specs/call-history.md). */
class HistoryViewModel(
    private val history: CallHistoryRepository,
    private val contacts: ContactDirectory,
    private val callLog: CallLogSync,
    private val normalizer: PhoneNumberNormalizer,
    private val clock: Clock,
    private val timeZone: TimeZone,
) : ViewModel() {
    private val view = MutableStateFlow(ViewState())

    val state: StateFlow<HistoryUiState> =
        combine(history.calls, view, callLog.callLogGranted) { calls, view, callLogGranted ->
            val names = contacts.names(calls.mapNotNull { it.number })
            val entries = calls.map { it.toEntry(names[it.number]) }
            val today = clock.now().toLocalDateTime(timeZone).date
            HistoryUiState(
                loading = false,
                filter = view.filter,
                totalCount = calls.size,
                days =
                    entries
                        .filter { it.matches(view.filter) }
                        .groupBy { it.date }
                        .map { (date, dayEntries) -> HistoryDay(date.label(today), dayEntries) },
                selected = entries.firstOrNull { it.id == view.selectedId },
                confirmingClear = view.confirmingClear,
                callLogPrompt =
                    when {
                        callLogGranted || !callLog.callLogAvailable -> CallLogPrompt.NONE
                        view.callLogDenied -> CallLogPrompt.DENIED
                        else -> CallLogPrompt.ASK
                    },
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HistoryUiState())

    init {
        // Calls since the app was last opened: learn from the call log how they ended, and bring in the others.
        viewModelScope.launch { callLog.sync() }
    }

    fun setFilter(value: HistoryFilter) = view.update { it.copy(filter = value) }

    fun select(id: Long) = view.update { it.copy(selectedId = id) }

    fun dismissDetails() = view.update { it.copy(selectedId = null) }

    fun delete(id: Long) {
        view.update { it.copy(selectedId = null) }
        viewModelScope.launch { history.delete(id) }
    }

    fun requestClear() = view.update { it.copy(confirmingClear = true) }

    fun cancelClear() = view.update { it.copy(confirmingClear = false) }

    fun clear() {
        view.update { it.copy(confirmingClear = false) }
        viewModelScope.launch { history.clear() }
    }

    fun allowCallLog() {
        viewModelScope.launch {
            val granted = callLog.requestCallLogAccess()
            view.update { it.copy(callLogDenied = !granted) }
        }
    }

    fun openCallLogSettings() = callLog.openCallLogSettings()

    private fun CallRecord.toEntry(name: String?): HistoryEntry {
        val local = timestamp.toLocalDateTime(timeZone)
        return HistoryEntry(
            id = id,
            number = number?.let(normalizer::format),
            name = name,
            outcome = outcome,
            reason = reason,
            rule = rule,
            fromCallLog = source == CallSource.CALL_LOG,
            date = local.date,
            time = "${local.hour.twoDigits()}:${local.minute.twoDigits()}",
        )
    }

    private data class ViewState(
        val filter: HistoryFilter = HistoryFilter.ALL,
        val selectedId: Long? = null,
        val confirmingClear: Boolean = false,
        val callLogDenied: Boolean = false,
    )
}

private fun HistoryEntry.matches(filter: HistoryFilter) =
    when (filter) {
        HistoryFilter.ALL -> true
        HistoryFilter.ANSWERED -> outcome == CallOutcome.ANSWERED
        HistoryFilter.MISSED -> outcome == CallOutcome.MISSED
        HistoryFilter.REJECTED -> outcome == CallOutcome.REJECTED
    }

private fun LocalDate.label(today: LocalDate): DayLabel =
    when (this) {
        today -> DayLabel.Today
        today.minus(1, DateTimeUnit.DAY) -> DayLabel.Yesterday
        else -> DayLabel.Date(this, showYear = year != today.year)
    }

private fun Int.twoDigits() = toString().padStart(2, '0')
