package app.knobly.platform.fake

import app.knobly.core.model.PhoneNumber
import app.knobly.domain.port.ContactsAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake contacts for the desktop harness and tests. Contacts are matched by national number (e.g. `11912345678`).
 * [grantOnRequest] decides what happens when the app asks for the permission.
 */
class FakeContactsAccess(
    contacts: Set<String> = setOf(DEFAULT_CONTACT),
    granted: Boolean = true,
    var grantOnRequest: Boolean = true,
) : ContactsAccess {
    private val grantedState = MutableStateFlow(granted)
    override val isGranted: StateFlow<Boolean> = grantedState.asStateFlow()
    var contacts: Set<String> = contacts
    var settingsOpened = 0
        private set

    override suspend fun requestAccess(): Boolean {
        if (grantOnRequest) grantedState.value = true
        return grantedState.value
    }

    override fun openAppSettings() {
        settingsOpened++
    }

    override fun isContact(number: PhoneNumber): Boolean = number.national in contacts

    /** Simulates the user granting or revoking the permission in the system settings. */
    fun setGranted(value: Boolean) {
        grantedState.value = value
    }

    companion object {
        /** (11) 91234-5678 */
        const val DEFAULT_CONTACT = "11912345678"
    }
}
