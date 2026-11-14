package app.knobly.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knobly.domain.port.ScreeningRoleController
import app.knobly.domain.rules.call.CallFilterSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Calls module status on the control center. */
data class CallsStatus(
    /** Knobly holds the call screening role. */
    val screeningOn: Boolean = false,
    val activeFilterCount: Int = 0,
) {
    /** Calls are actually being filtered: the role is held and at least one filter is on. */
    val isProtected: Boolean get() = screeningOn && activeFilterCount > 0
}

class HomeViewModel(
    private val roleController: ScreeningRoleController,
    settings: CallFilterSettingsRepository,
) : ViewModel() {
    val isCallFilteringAvailable: Boolean = roleController.isRoleAvailable

    val callsStatus: StateFlow<CallsStatus> =
        combine(
            roleController.isRoleHeld,
            settings.settings,
        ) { held, filters -> CallsStatus(held, filters.activeFilterCount) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CallsStatus(screeningOn = roleController.isRoleHeld.value))

    fun enableCallFiltering() {
        viewModelScope.launch { roleController.requestRole() }
    }
}
