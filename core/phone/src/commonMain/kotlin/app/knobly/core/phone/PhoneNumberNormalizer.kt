package app.knobly.core.phone

import app.knobly.core.model.PhoneNumber

/** Turns the caller id received from the platform into a [PhoneNumber], and back into text for the UI. */
fun interface PhoneNumberNormalizer {
    /** Returns `null` when the number is hidden/withheld (see [HiddenNumbers]). */
    fun normalize(raw: String?): PhoneNumber?

    /**
     * How the number is shown to the user: national format for numbers of the default region (`(11) 91234-5678`),
     * international otherwise (`+1 650-253-0000`). Falls back to the received digits.
     */
    fun format(number: PhoneNumber): String = number.raw
}

/** Recognizes caller ids that mean "no number": withheld, private or unknown callers. */
object HiddenNumbers {
    private val hiddenMarkers = setOf("anonymous", "private", "restricted", "unknown", "unavailable", "-1", "-2", "-3")

    fun isHidden(raw: String?): Boolean {
        val value = raw?.trim().orEmpty()
        return value.isEmpty() || value.lowercase() in hiddenMarkers || value.none { it.isDigit() }
    }
}
