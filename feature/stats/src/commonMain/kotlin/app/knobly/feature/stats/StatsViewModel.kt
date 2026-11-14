package app.knobly.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knobly.domain.stats.CallStatistics
import app.knobly.domain.stats.CallStats
import app.knobly.domain.stats.StatsPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.DAY,
    /** `null` while loading. */
    val stats: CallStats? = null,
)

/** The Statistics tab (docs/specs/call-statistics.md). Opens on the last 7 days, as in the mockup. */
class StatsViewModel(
    private val statistics: CallStatistics,
) : ViewModel() {
    private val period = MutableStateFlow(StatsPeriod.DAY)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<StatsUiState> =
        period
            .flatMapLatest { period -> statistics.stats(period).map { StatsUiState(period, it) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, StatsUiState())

    fun setPeriod(value: StatsPeriod) = period.update { value }
}
