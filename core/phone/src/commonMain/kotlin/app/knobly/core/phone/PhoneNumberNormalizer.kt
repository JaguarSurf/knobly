package app.knobly.core.phone

import app.knobly.core.model.PhoneNumber

/** Turns the caller id received from the platform into a [PhoneNumber]. */
fun interface PhoneNumberNormalizer {
    /** Returns `null` when the number is hidden/withheld (see [HiddenNumbers]). */
    fun normalize(raw: String?): PhoneNumber?
}

/** Recognizes caller ids that mean "no number": withheld, private or unknown callers. */
object HiddenNumbers {
    private val hiddenMarkers = setOf("anonymous", "private", "restricted", "unknown", "unavailable", "-1", "-2", "-3")

    fun isHidden(raw: String?): Boolean {
        val value = raw?.trim().orEmpty()
        return value.isEmpty() || value.lowercase() in hiddenMarkers || value.none { it.isDigit() }
    }
}
