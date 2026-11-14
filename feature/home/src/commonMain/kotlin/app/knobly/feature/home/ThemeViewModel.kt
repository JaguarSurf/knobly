package app.knobly.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knobly.domain.settings.AppSettingsRepository
import app.knobly.domain.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The theme choice on the Home header (docs/specs/appearance.md). The app applies it everywhere. */
class ThemeViewModel(
    private val settings: AppSettingsRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> =
        settings.themeMode
            .map { it ?: ThemeMode.SYSTEM }
            .stateIn(viewModelScope, SharingStarted.Eagerly, settings.themeMode.value ?: ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }
}
