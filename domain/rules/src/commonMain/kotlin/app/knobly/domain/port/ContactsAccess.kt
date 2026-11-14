package app.knobly.domain.port

import app.knobly.core.model.PhoneNumber
import kotlinx.coroutines.flow.StateFlow

/**
 * Port: access to the user's contacts (permission and lookup).
 *
 * Android: `READ_CONTACTS` + `ContactsContract.PhoneLookup`. iOS: not used for filtering (contacts always win there).
 * Desktop harness and tests: a fake.
 */
interface ContactsAccess {
    val isGranted: StateFlow<Boolean>

    /** Asks for the permission if needed. Returns whether it is granted afterwards. */
    suspend fun requestAccess(): Boolean

    /** Opens the system screen where the user can grant a permission that was denied. */
    fun openAppSettings()

    /** Whether [number] is saved in the contacts. May block: never call on the main thread. */
    fun isContact(number: PhoneNumber): Boolean
}
