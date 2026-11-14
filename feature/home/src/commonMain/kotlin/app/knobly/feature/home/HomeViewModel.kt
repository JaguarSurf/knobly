package app.knobly.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knobly.domain.port.ScreeningRoleController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State of the control center: for now, whether call filtering (the call screening role) is on. */
class HomeViewModel(
    private val roleController: ScreeningRoleController,
) : ViewModel() {
    val isCallFilteringAvailable: Boolean = roleController.isRoleAvailable

    val isCallFilteringOn: StateFlow<Boolean> =
        roleController.isRoleHeld.stateIn(viewModelScope, SharingStarted.Eagerly, roleController.isRoleHeld.value)

    fun enableCallFiltering() {
        viewModelScope.launch { roleController.requestRole() }
    }
}
