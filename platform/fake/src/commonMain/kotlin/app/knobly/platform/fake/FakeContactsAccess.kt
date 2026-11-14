package app.knobly.platform.fake

import app.knobly.domain.port.ContactsAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake contacts for the desktop harness and tests: numbers as a user would save them (any format). Matching is done
 * by the domain, as with real contacts. [grantOnRequest] decides what happens when the app asks for the permission.
 */
class FakeContactsAccess(
    contacts: List<String> = DEFAULT_CONTACTS,
    granted: Boolean = true,
    var grantOnRequest: Boolean = true,
) : ContactsAccess {
    private val grantedState = MutableStateFlow(granted)
    private var version = 0
    override val isGranted: StateFlow<Boolean> = grantedState.asStateFlow()

    /** Changing the contacts changes [contactsVersion], like the system contacts do. */
    var contacts: List<String> = contacts
        set(value) {
            field = value
            version++
        }

    var settingsOpened = 0
        private set

    override suspend fun requestAccess(): Boolean {
        if (grantOnRequest) grantedState.value = true
        return grantedState.value
    }

    override fun openAppSettings() {
        settingsOpened++
    }

    override fun phoneNumbers(): List<String> = if (grantedState.value) contacts else emptyList()

    override fun contactsVersion(): String = version.toString()

    /** Simulates the user granting or revoking the permission in the system settings. */
    fun setGranted(value: Boolean) {
        grantedState.value = value
    }

    companion object {
        /** One contact saved in full, one saved the old way (without the DDD's 9th digit), as in many phones. */
        val DEFAULT_CONTACTS = listOf("(11) 91234-5678", "(35) 8123-4567")
    }
}
