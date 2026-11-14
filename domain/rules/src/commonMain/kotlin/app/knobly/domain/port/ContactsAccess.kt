package app.knobly.domain.port

import kotlinx.coroutines.flow.StateFlow

/** A phone number saved in the contacts, and the name of its contact. */
data class ContactNumber(
    val number: String,
    val name: String? = null,
)

/**
 * Port: access to the user's contacts (permission and their phone numbers).
 *
 * Only reads the numbers (and names) as the user saved them: matching a call against them (with or without the DDD,
 * the country code or the 9th digit) is done by the domain, see `ContactDirectory` and
 * docs/specs/phone-number-matching.md.
 *
 * Android: `READ_CONTACTS` + `ContactsContract`. iOS: not used for filtering (contacts always win there).
 * Desktop harness and tests: a fake.
 */
interface ContactsAccess {
    val isGranted: StateFlow<Boolean>

    /** Asks for the permission if needed. Returns whether it is granted afterwards. */
    suspend fun requestAccess(): Boolean

    /** Opens the system screen where the user can grant a permission that was denied. */
    fun openAppSettings()

    /** Every phone number in the contacts, as saved, with its contact's name. Empty without the permission. Blocks. */
    fun phoneNumbers(): List<ContactNumber>

    /**
     * A value that changes whenever the contacts change (added, edited or deleted). Read on every call, so it must be
     * cheap. Blocks: never on the main thread.
     */
    fun contactsVersion(): String
}
