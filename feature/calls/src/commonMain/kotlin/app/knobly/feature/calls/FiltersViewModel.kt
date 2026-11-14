package app.knobly.feature.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knobly.domain.port.ContactsAccess
import app.knobly.domain.rules.call.CallFilterSettings
import app.knobly.domain.rules.call.CallFilterSettingsRepository
import app.knobly.domain.rules.call.CustomRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the Filters tab shows. */
data class FiltersUiState(
    val contactsOnly: Boolean = false,
    val customRulesEnabled: Boolean = false,
    val rules: List<CustomRule> = emptyList(),
    val activeFilterCount: Int = 0,
    /** "Contacts only" cannot work: the permission was just denied, or revoked while the filter is on. */
    val showContactsPermissionMessage: Boolean = false,
    /** Custom rules are on but contacts cannot be protected from them. */
    val showContactsNotProtectedNote: Boolean = false,
)

/** The Filters tab (docs/specs/call-filters.md): two toggles, the custom rules and the contacts permission. */
class FiltersViewModel(
    private val repository: CallFilterSettingsRepository,
    private val contacts: ContactsAccess,
) : ViewModel() {
    private val contactsDenied = MutableStateFlow(false)

    val state: StateFlow<FiltersUiState> =
        combine(repository.settings, contacts.isGranted, contactsDenied) { settings, granted, denied ->
            settings.toUiState(granted, denied)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, FiltersUiState())

    /** Turning "contacts only" on asks for the contacts permission first; without it the filter stays off. */
    fun setContactsOnly(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val granted = contacts.requestAccess()
                contactsDenied.value = !granted
                if (!granted) return@launch
            }
            repository.setContactsOnly(enabled)
        }
    }

    /** Custom rules work without contacts, but the permission is requested so contacts can always pass. */
    fun setCustomRulesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setCustomRulesEnabled(enabled)
            if (enabled && !contacts.isGranted.value) contacts.requestAccess()
        }
    }

    fun setRuleEnabled(
        id: String,
        enabled: Boolean,
    ) {
        viewModelScope.launch { repository.setRuleEnabled(id, enabled) }
    }

    fun openContactsSettings() = contacts.openAppSettings()

    private fun CallFilterSettings.toUiState(
        contactsGranted: Boolean,
        contactsDenied: Boolean,
    ) = FiltersUiState(
        contactsOnly = contactsOnly,
        customRulesEnabled = customRulesEnabled,
        rules = customRules,
        // Without the permission "contacts only" does nothing (it fails open), so it does not count as active.
        activeFilterCount = activeFilterCount - if (contactsOnly && !contactsGranted) 1 else 0,
        showContactsPermissionMessage = !contactsGranted && (contactsDenied || contactsOnly),
        showContactsNotProtectedNote = !contactsGranted && customRulesEnabled && customRules.isNotEmpty(),
    )
}
