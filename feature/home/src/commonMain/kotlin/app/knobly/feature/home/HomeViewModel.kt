package app.knobly.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.port.ScreeningRoleController
import app.knobly.domain.rules.call.CallFilterSettingsRepository
import app.knobly.domain.stats.CallStatistics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Calls module status on the control center. */
data class CallsStatus(
    /** Knobly holds the call screening role. */
    val screeningOn: Boolean = false,
    val activeFilterCount: Int = 0,
) {
    /** Calls are actually being filtered: the role is held and at least one filter is on. */
    val isProtected: Boolean get() = screeningOn && activeFilterCount > 0
}

/** Rejected calls on the control center: today, in the last 7 days, and the latest one. */
data class CallsActivity(
    val today: Int = 0,
    val lastSevenDays: Int = 0,
    val latest: LatestRejection? = null,
)

data class LatestRejection(
    /** Formatted for display; `null` when the number was hidden. */
    val number: String?,
    val date: LocalDate,
    /** `HH:mm`, local time. */
    val time: String,
    /** 0 = today, 1 = yesterday… */
    val daysAgo: Int,
)

class HomeViewModel(
    private val roleController: ScreeningRoleController,
    settings: CallFilterSettingsRepository,
    statistics: CallStatistics,
    normalizer: PhoneNumberNormalizer,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {
    val isCallFilteringAvailable: Boolean = roleController.isRoleAvailable

    /** Greeting of the moment the screen opens ("Bom dia"…). */
    val dayPart: DayPart = DayPart.fromHour(clock.now().toLocalDateTime(timeZone).hour)

    val callsStatus: StateFlow<CallsStatus> =
        combine(
            roleController.isRoleHeld,
            settings.settings,
        ) { held, filters -> CallsStatus(held, filters.activeFilterCount) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CallsStatus(screeningOn = roleController.isRoleHeld.value))

    val activity: StateFlow<CallsActivity> =
        statistics
            .summary()
            .map { summary ->
                val today = clock.now().toLocalDateTime(timeZone).date
                CallsActivity(
                    today = summary.today,
                    lastSevenDays = summary.lastSevenDays,
                    latest =
                        summary.latest?.let { call ->
                            val local = call.timestamp.toLocalDateTime(timeZone)
                            LatestRejection(
                                number = call.number?.let(normalizer::format),
                                date = local.date,
                                time = "${local.hour.toString().padStart(
                                    2,
                                    '0',
                                )}:${local.minute.toString().padStart(2, '0')}",
                                daysAgo = local.date.daysUntil(today),
                            )
                        },
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, CallsActivity())

    fun enableCallFiltering() {
        viewModelScope.launch { roleController.requestRole() }
    }
}
