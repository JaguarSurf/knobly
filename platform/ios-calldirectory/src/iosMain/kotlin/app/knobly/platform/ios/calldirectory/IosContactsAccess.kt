package app.knobly.platform.ios.calldirectory

import app.knobly.domain.port.ContactNumber
import app.knobly.domain.port.ContactsAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS never asks Knobly about individual calls, and contacts always take precedence over Call Directory blocks, so
 * the app does not need to read the contacts there. See docs/platforms.md.
 */
class IosContactsAccess : ContactsAccess {
    override val isGranted: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override suspend fun requestAccess(): Boolean = false

    override fun openAppSettings() = Unit

    override fun phoneNumbers(): List<ContactNumber> = emptyList()

    override fun contactsVersion(): String = ""
}
