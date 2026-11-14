package app.knobly.domain.rules.call

import app.knobly.core.model.PhoneNumber
import app.knobly.core.phone.PhoneNumberMatching
import app.knobly.core.phone.PhoneNumberNormalizer
import app.knobly.domain.port.ContactNumber
import app.knobly.domain.port.ContactsAccess
import app.knobly.domain.rules.ContactLookup
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile

/**
 * The user's contact numbers in memory, indexed by their last 8 digits, so a call is matched without reading every
 * contact while the phone is ringing. Numbers match with or without the country code, the DDD and the 9th digit
 * ([PhoneNumberMatching], docs/specs/phone-number-matching.md).
 *
 * The index is built in the background as soon as the contacts can be read. On every call, a cheap
 * [ContactsAccess.contactsVersion] tells whether the contacts changed since; if so, the index is rebuilt first.
 * (Change notifications are not enough: Android freezes the app's process between calls and delivers them late.)
 */
class ContactDirectory(
    private val contacts: ContactsAccess,
    private val normalizer: PhoneNumberNormalizer,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) : ContactLookup {
    @Volatile
    private var current: VersionedIndex? = null

    override val isAvailable: Boolean get() = contacts.isGranted.value

    init {
        warmUp()
        // Undispatched: subscribed before the constructor returns, so a permission change is never missed.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            contacts.isGranted.drop(1).collect {
                current = null
                warmUp()
            }
        }
    }

    override fun isContact(number: PhoneNumber): Boolean = isAvailable && index().find(number) != null

    /** The contact names of [numbers] (History), for the numbers that are saved. Reads the contacts if they changed. */
    suspend fun names(numbers: Collection<PhoneNumber>): Map<PhoneNumber, String> =
        if (!isAvailable || numbers.isEmpty()) {
            emptyMap()
        } else {
            withContext(dispatcher) {
                val index = index()
                numbers.distinct().mapNotNull { number -> index.find(number)?.name?.let { number to it } }.toMap()
            }
        }

    private fun warmUp() {
        if (isAvailable) scope.launch(dispatcher) { index() }
    }

    private fun index(): ContactIndex {
        val version = contacts.contactsVersion()
        current?.takeIf { it.version == version }?.let { return it.index }
        return ContactIndex(contacts.phoneNumbers(), normalizer).also { current = VersionedIndex(version, it) }
    }

    private class VersionedIndex(
        val version: String,
        val index: ContactIndex,
    )
}

/** Contact numbers by their last 8 digits; candidates are parsed only when a call has the same ending. */
internal class ContactIndex(
    numbers: List<ContactNumber>,
    private val normalizer: PhoneNumberNormalizer,
) {
    private val byKey: Map<String, List<ContactNumber>> =
        numbers
            .map { it.copy(number = dialableDigits(it.number)) }
            .filter { it.number.isNotEmpty() }
            .distinct()
            .groupBy { PhoneNumberMatching.indexKey(it.number) }

    /** The saved entry for [number], if any (preferring one with a name). */
    fun find(number: PhoneNumber): ContactNumber? =
        byKey[PhoneNumberMatching.indexKey(number.national)]
            .orEmpty()
            .filter { saved ->
                normalizer.normalize(saved.number)?.let { PhoneNumberMatching.sameNumber(number, it) }
                    ?: false
            }.maxByOrNull { !it.name.isNullOrBlank() }

    private companion object {
        /** `+55 (35) 8123-4567 ramal 12` -> `+553581234567`: digits up to an extension, pause or wait. */
        fun dialableDigits(saved: String): String {
            val number = saved.trim().takeWhile { it != ',' && it != ';' && !it.isLetter() }
            val digits = number.filter(Char::isDigit)
            return if (number.startsWith("+") && digits.isNotEmpty()) "+$digits" else digits
        }
    }
}
