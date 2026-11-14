package app.knobly.domain.rules.call

import app.knobly.domain.port.ContactsAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Contacts as saved by a user (any format), for domain tests. Counts how often they are read. */
internal class TestContacts(
    numbers: List<String>,
    granted: Boolean = true,
) : ContactsAccess {
    private val grantedState = MutableStateFlow(granted)
    private var version = 0
    override val isGranted: StateFlow<Boolean> = grantedState

    var numbers: List<String> = numbers
        set(value) {
            field = value
            version++
        }

    var reads = 0
        private set

    override suspend fun requestAccess(): Boolean = grantedState.value

    override fun openAppSettings() = Unit

    override fun contactsVersion(): String = version.toString()

    override fun phoneNumbers(): List<String> {
        reads++
        return if (grantedState.value) numbers else emptyList()
    }
}
