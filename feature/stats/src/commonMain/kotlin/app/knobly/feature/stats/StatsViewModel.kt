package app.knobly.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knobly.domain.history.CallOutcomeResolver
import app.knobly.domain.stats.CallCategory
import app.knobly.domain.stats.CallStatistics
import app.knobly.domain.stats.CallStats
import app.knobly.domain.stats.StatsPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.DAY,
    val category: CallCategory = CallCategory.ALL,
    /** `null` while loading. */
    val stats: CallStats? = null,
    /** Some calls only "rang" and the call log cannot be read: offer the permission. */
    val askCallLog: Boolean = false,
    val callLogDenied: Boolean = false,
)

/** The Statistics tab (docs/specs/call-statistics.md). Opens on every call of the last 7 days. */
class StatsViewModel(
    private val statistics: CallStatistics,
    private val outcomes: CallOutcomeResolver,
) : ViewModel() {
    private val period = MutableStateFlow(StatsPeriod.DAY)
    private val category = MutableStateFlow(CallCategory.ALL)
    private val callLogDenied = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<StatsUiState> =
        combine(
            period.flatMapLatest { statistics.stats(it) },
            category,
            outcomes.callLogGranted,
            callLogDenied,
        ) { stats, category, granted, denied ->
            StatsUiState(
                period = stats.period,
                category = category,
                stats = stats,
                askCallLog = !granted && stats.totals.rang > 0,
                callLogDenied = denied,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, StatsUiState())

    init {
        viewModelScope.launch { outcomes.resolvePending() }
    }

    fun setPeriod(value: StatsPeriod) = period.update { value }

    fun setCategory(value: CallCategory) = category.update { value }

    fun allowCallLog() {
        viewModelScope.launch { callLogDenied.value = !outcomes.requestCallLogAccess() }
    }

    fun openCallLogSettings() = outcomes.openCallLogSettings()
}
