package app.knobly.domain.rules.call

import app.knobly.core.model.PhoneNumber
import app.knobly.domain.port.ContactsAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Contacts by E.164 number, for domain tests. */
internal class TestContacts(
    private val numbers: Set<String>,
    granted: Boolean = true,
) : ContactsAccess {
    private val grantedState = MutableStateFlow(granted)
    override val isGranted: StateFlow<Boolean> = grantedState
    var lookups = 0
        private set

    override suspend fun requestAccess(): Boolean = grantedState.value

    override fun openAppSettings() = Unit

    override fun isContact(number: PhoneNumber): Boolean {
        lookups++
        return number.e164 in numbers
    }
}
