package app.knobly.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knobly.domain.port.ScreeningRoleController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Whether call filtering is enabled, i.e. whether Knobly holds the call screening role, and how to enable it.
 * Uses the [ScreeningRoleController] port (never a platform API).
 */
class CallFilteringViewModel(
    private val roleController: ScreeningRoleController,
) : ViewModel() {
    val isRoleAvailable: Boolean = roleController.isRoleAvailable

    val isRoleHeld: StateFlow<Boolean> =
        roleController.isRoleHeld.stateIn(viewModelScope, SharingStarted.Eagerly, roleController.isRoleHeld.value)

    fun requestRole() {
        viewModelScope.launch { roleController.requestRole() }
    }
}
